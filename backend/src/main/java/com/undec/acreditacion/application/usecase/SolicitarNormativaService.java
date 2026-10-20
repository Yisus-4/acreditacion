package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SolicitarNormativaService implements SolicitarNormativaUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final SolicitudNormativaRepositoryPort solicitudRepository;
    private final DocumentoNormativaRepositoryPort documentoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private final EmailNotificationPort emailNotificationPort;

    public SolicitarNormativaService(
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
    public NormativaDetalleDto ejecutar(SolicitarNormativaCommand command) {
        Objects.requireNonNull(command, "El comando no puede ser nulo");
        UUID procesoId = command.procesoId();

        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Proceso de acreditación no encontrado con ID: " + procesoId));

        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan de trabajo no encontrado para el proceso ID: " + procesoId));

        ActividadPlanTrabajo actividadNormativa = NormativaMapperHelper.resolverActividadNormativa(plan);
        if (planTrabajoRepository.findActividadById(actividadNormativa.getId()).isEmpty()) {
            ActividadPlanTrabajo guardada = planTrabajoRepository.saveActividad(actividadNormativa);
            if (guardada != null) {
                actividadNormativa = guardada;
            }
        }

        final UUID finalActividadId = actividadNormativa.getId();
        SolicitudNormativaCarrera solicitud = solicitudRepository.findByActividadId(finalActividadId)
            .orElseGet(() -> SolicitudNormativaCarrera.iniciar(proceso.getId(), finalActividadId));

        solicitud.solicitar(command.solicitanteId(), command.observaciones(), 5, LocalDate.now());
        SolicitudNormativaCarrera guardada = solicitudRepository.save(solicitud);

        actividadNormativa.cambiarEstado(EstadoActividad.EN_PROGRESO);
        planTrabajoRepository.saveActividad(actividadNormativa);

        String carreraNombre = carreraRepository.findById(proceso.getCarreraId())
            .map(Carrera::getNombre)
            .orElse("Carrera UNdeC");

        String codigoConvocatoria = registroConvocatoriaRepository.findByProcesoId(proceso.getId())
            .map(RegistroConvocatoria::getNroResolucionConeau)
            .orElse(proceso.getCodigo());

        String emailDestino = "director_carrera@undec.edu.ar";
        String nombreDestino = "Dirección de Carrera";

        emailNotificationPort.enviarSolicitudNormativa(
            proceso.getId(),
            emailDestino,
            nombreDestino,
            carreraNombre,
            codigoConvocatoria,
            guardada.getFechaLimiteEntrega(),
            guardada.getObservacionesSolicitud()
        );

        List<DocumentoNormativa> docs = documentoRepository.findBySolicitudNormativaId(guardada.getId());
        return NormativaMapperHelper.toDto(guardada, docs, proceso);
    }
}
