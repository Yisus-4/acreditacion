package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.DocumentoObservadoDto;
import com.undec.acreditacion.application.input.NormativaDetalleDto;
import com.undec.acreditacion.application.input.RevisarNormativaCommand;
import com.undec.acreditacion.application.input.RevisarNormativaUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
            solicitud.observar(command.revisorId(), command.observacionesRevision());

            List<String> nombresObservados = new ArrayList<>();
            if (command.documentosObservados() != null) {
                for (DocumentoObservadoDto obs : command.documentosObservados()) {
                    documentoRepository.findById(obs.documentoId()).ifPresent(doc -> {
                        doc.marcarObservado(obs.motivo());
                        documentoRepository.save(doc);
                        nombresObservados.add(doc.getNombreOriginal() + " (" + obs.motivo() + ")");
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
                command.observacionesRevision(),
                nombresObservados
            );
        } else {
            solicitud.aprobar(command.revisorId(), command.observacionesRevision());
            solicitudRepository.save(solicitud);

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
