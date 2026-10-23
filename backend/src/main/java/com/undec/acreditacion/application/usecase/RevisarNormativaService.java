package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.DocumentoObservadoDto;
import com.undec.acreditacion.application.input.NormativaDetalleDto;
import com.undec.acreditacion.application.input.RevisarNormativaCommand;
import com.undec.acreditacion.application.input.RevisarNormativaUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class RevisarNormativaService implements RevisarNormativaUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final SolicitudNormativaRepositoryPort solicitudRepository;
    private final DocumentoNormativaRepositoryPort documentoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private final EmailNotificationPort emailNotificationPort;

    public RevisarNormativaService(
        ProcesoAcreditacionRepositoryPort procesoRepository,
        PlanTrabajoRepositoryPort planTrabajoRepository,
        SolicitudNormativaRepositoryPort solicitudRepository,
        DocumentoNormativaRepositoryPort documentoRepository,
        CarreraRepositoryPort carreraRepository,
        RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
        EmailNotificationPort emailNotificationPort
    ) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.solicitudRepository = Objects.requireNonNull(solicitudRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.registroConvocatoriaRepository = Objects.requireNonNull(registroConvocatoriaRepository);
        this.emailNotificationPort = Objects.requireNonNull(emailNotificationPort);
    }

    @Override
    public NormativaDetalleDto ejecutar(RevisarNormativaCommand command) {
        Objects.requireNonNull(command, "El comando no puede ser nulo");
        UUID procesoId = command.procesoId();

        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Proceso no encontrado con ID: " + procesoId));

        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan de trabajo no encontrado para el proceso ID: " + procesoId));

        ActividadPlanTrabajo actividadNormativa = NormativaMapperHelper.resolverActividadNormativa(plan);

        SolicitudNormativaCarrera solicitud = solicitudRepository.findByActividadId(actividadNormativa.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Solicitud de normativa no encontrada"));

        String carreraNombre = carreraRepository.findById(proceso.getCarreraId())
            .map(Carrera::getNombre)
            .orElse("Carrera UNdeC");

        String codigoConvocatoria = registroConvocatoriaRepository.findByProcesoId(proceso.getId())
            .map(RegistroConvocatoria::getNroResolucionConeau)
            .orElse(proceso.getCodigo());

        if (command.decision() == DecisionRevisionNormativa.OBSERVAR) {
            String rawObs = command.observacionesRevision() != null ? command.observacionesRevision().trim() : "";

            Map<UUID, String> motivosPorDocId = new LinkedHashMap<>();
            if (command.documentosObservados() != null) {
                for (DocumentoObservadoDto obs : command.documentosObservados()) {
                    if (obs != null && obs.documentoId() != null) {
                        String motivoDoc = (obs.motivo() != null && !obs.motivo().trim().isEmpty())
                            ? obs.motivo().trim()
                            : (!rawObs.isEmpty() ? rawObs : "Requiere corrección");
                        motivosPorDocId.put(obs.documentoId(), motivoDoc);
                    }
                }
            }

            String motivoGeneral = rawObs;
            if (motivoGeneral.isEmpty() && !motivosPorDocId.isEmpty()) {
                motivoGeneral = String.join("; ", motivosPorDocId.values());
            }

            solicitud.observar(command.revisorId(), motivoGeneral);

            List<String> nombresObservados = new ArrayList<>();
            List<DocumentoNormativa> todosDocs = documentoRepository.findBySolicitudNormativaId(solicitud.getId());
            Set<UUID> procesados = new HashSet<>();

            if (todosDocs != null) {
                for (DocumentoNormativa doc : todosDocs) {
                    if (motivosPorDocId.containsKey(doc.getId())) {
                        String motivo = motivosPorDocId.get(doc.getId());
                        doc.marcarObservado(motivo);
                        documentoRepository.save(doc);
                        nombresObservados.add(doc.getNombreOriginal() + " (" + motivo + ")");
                        procesados.add(doc.getId());
                    } else if (doc.isObservado()) {
                        doc.limpiarObservacion();
                        documentoRepository.save(doc);
                    }
                }
            }

            for (Map.Entry<UUID, String> entry : motivosPorDocId.entrySet()) {
                if (!procesados.contains(entry.getKey())) {
                    documentoRepository.findById(entry.getKey()).ifPresent(doc -> {
                        doc.marcarObservado(entry.getValue());
                        documentoRepository.save(doc);
                        nombresObservados.add(doc.getNombreOriginal() + " (" + entry.getValue() + ")");
                    });
                }
            }

            solicitudRepository.save(solicitud);

            emailNotificationPort.enviarObservaciones(
                proceso.getId(),
                "director_carrera@undec.edu.ar",
                "Dirección de Carrera",
                carreraNombre,
                codigoConvocatoria,
                solicitud.getObservacionesRevision(),
                nombresObservados
            );
        } else {
            solicitud.aprobar(command.revisorId(), command.observacionesRevision());
            solicitudRepository.save(solicitud);

            List<DocumentoNormativa> todosDocs = documentoRepository.findBySolicitudNormativaId(solicitud.getId());
            if (todosDocs != null) {
                for (DocumentoNormativa doc : todosDocs) {
                    if (doc.isObservado()) {
                        doc.limpiarObservacion();
                        documentoRepository.save(doc);
                    }
                }
            }

            actividadNormativa.completar(command.revisorId());
            planTrabajoRepository.saveActividad(actividadNormativa);

            emailNotificationPort.enviarAprobacion(
                proceso.getId(),
                "director_carrera@undec.edu.ar",
                "Dirección de Carrera",
                carreraNombre,
                codigoConvocatoria,
                command.observacionesRevision()
            );
        }

        List<DocumentoNormativa> documentos = documentoRepository.findBySolicitudNormativaId(solicitud.getId());
        return NormativaMapperHelper.toDto(solicitud, documentos, proceso);
    }
}
