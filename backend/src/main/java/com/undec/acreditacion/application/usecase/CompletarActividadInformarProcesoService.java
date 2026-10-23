package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CompletarActividadCommand;
import com.undec.acreditacion.application.input.CompletarActividadInformarProcesoUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Servicio de aplicación PURO (POJO).
 * Sin anotaciones @Service ni @Transactional (H-02).
 * Resuelve procesoId navegando la relación hacia PlanTrabajo (H-01).
 * Implementa compensación en MinIO si la persistencia en base de datos aborta.
 */
public class CompletarActividadInformarProcesoService implements CompletarActividadInformarProcesoUseCase {

    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final DocumentoAcreditacionRepositoryPort documentoRepository;
    private final DocumentStoragePort documentStoragePort;

    public CompletarActividadInformarProcesoService(
            PlanTrabajoRepositoryPort planTrabajoRepository,
            DocumentoAcreditacionRepositoryPort documentoRepository,
            DocumentStoragePort documentStoragePort) {
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.documentStoragePort = Objects.requireNonNull(documentStoragePort);
    }

    @Override
    public ActividadPlanTrabajo completar(CompletarActividadCommand command) {
        // 1. Recuperar la actividad
        ActividadPlanTrabajo actividad = planTrabajoRepository.findActividadById(command.actividadId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la actividad con ID: " + command.actividadId()));

        // 2. H-01: Resolver procesoId navegando la relación hacia el Plan de Trabajo
        PlanTrabajo plan = planTrabajoRepository.findById(actividad.getPlanTrabajoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de trabajo con ID: " + actividad.getPlanTrabajoId()));
        UUID procesoId = plan.getProcesoId();

        // 3. Adjunto opcional: si no hay archivo, solo se registra la comunicación y se completa
        if (!command.tieneAdjunto()) {
            registrarComunicacion(actividad, command);
            return planTrabajoRepository.saveActividad(actividad);
        }

        // 4. Subir archivo a MinIO
        String filename = "ACTA_" + UUID.randomUUID() + ".pdf";
        String storagePath = documentStoragePort.uploadFile(command.archivoBytes(), filename, "application/pdf");

        try {
            // 5. Persistir metadatos del documento adjunto vinculados al procesoId
            DocumentoAcreditacion documento = DocumentoAcreditacion.crear(
                    procesoId,
                    actividad.getId(),
                    command.nombreArchivo(),
                    command.tipoDocumento(),
                    command.mimeType(),
                    command.archivoBytes().length,
                    storagePath,
                    command.usuarioAeaId()
            );
            documentoRepository.save(documento);

            // 6. Completar actividad en el dominio y persistir
            registrarComunicacion(actividad, command);
            return planTrabajoRepository.saveActividad(actividad);

        } catch (Exception ex) {
            // Compensación MinIO: Si aborta la persistencia de base de datos, eliminar el archivo para evitar huérfanos
            try {
                documentStoragePort.deleteFile(storagePath);
            } catch (Exception cleanupEx) {
                // Silenciar fallo secundario de compensación
            }
            throw ex;
        }
    }

    private void registrarComunicacion(ActividadPlanTrabajo actividad, CompletarActividadCommand command) {
        actividad.registrarComunicacionYCompletar(
                command.areasInformadas(),
                command.documentacionRemitida(),
                command.fechaReunion(),
                command.observaciones(),
                command.usuarioAeaId()
        );
    }
}
