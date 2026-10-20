package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EliminarDocumentoBorradorCommand;
import com.undec.acreditacion.application.input.EliminarDocumentoBorradorUseCase;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.DocumentoNormativa;
import com.undec.acreditacion.domain.entities.EstadoSolicitudNormativa;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.SolicitudNormativaCarrera;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.EstadoInvalidoException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class EliminarDocumentoBorradorService implements EliminarDocumentoBorradorUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final SolicitudNormativaRepositoryPort solicitudRepository;
    private final DocumentoNormativaRepositoryPort documentoRepository;
    private final DocumentStoragePort documentStoragePort;

    public EliminarDocumentoBorradorService(
        ProcesoAcreditacionRepositoryPort procesoRepository,
        PlanTrabajoRepositoryPort planTrabajoRepository,
        SolicitudNormativaRepositoryPort solicitudRepository,
        DocumentoNormativaRepositoryPort documentoRepository,
        DocumentStoragePort documentStoragePort
    ) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.solicitudRepository = Objects.requireNonNull(solicitudRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.documentStoragePort = Objects.requireNonNull(documentStoragePort);
    }

    @Override
    public void ejecutar(EliminarDocumentoBorradorCommand command) {
        Objects.requireNonNull(command, "El comando no puede ser nulo");
        UUID procesoId = command.procesoId();

        procesoRepository.findById(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Proceso no encontrado con ID: " + procesoId));

        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan de trabajo no encontrado para el proceso ID: " + procesoId));

        ActividadPlanTrabajo actividadNormativa = NormativaMapperHelper.resolverActividadNormativa(plan);

        SolicitudNormativaCarrera solicitud = solicitudRepository.findByActividadId(actividadNormativa.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Solicitud de normativa no encontrada"));

        if (solicitud.getEstado() == EstadoSolicitudNormativa.ENVIADA_A_REVISION || solicitud.getEstado() == EstadoSolicitudNormativa.COMPLETADA) {
            throw new EstadoInvalidoException("No se pueden eliminar documentos cuando la solicitud está en revisión o completada");
        }

        DocumentoNormativa documento = documentoRepository.findById(command.documentoId())
            .orElseThrow(() -> new ResourceNotFoundException("Documento normativo no encontrado con ID: " + command.documentoId()));

        if (!documento.getProcesoId().equals(procesoId)) {
            throw new DomainValidationException("El documento no pertenece al proceso especificado");
        }

        if (!documento.isEsBorrador()) {
            throw new EstadoInvalidoException("No se puede eliminar un documento definitivo/congelado");
        }

        if (documento.getStoragePath() != null) {
            documentStoragePort.deleteFile(documento.getStoragePath());
        }

        documentoRepository.delete(documento);
    }
}
