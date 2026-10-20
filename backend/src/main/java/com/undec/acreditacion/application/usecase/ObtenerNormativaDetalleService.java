package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.NormativaDetalleDto;
import com.undec.acreditacion.application.input.ObtenerNormativaDetalleUseCase;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ObtenerNormativaDetalleService implements ObtenerNormativaDetalleUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final SolicitudNormativaRepositoryPort solicitudRepository;
    private final DocumentoNormativaRepositoryPort documentoRepository;

    public ObtenerNormativaDetalleService(
        ProcesoAcreditacionRepositoryPort procesoRepository,
        PlanTrabajoRepositoryPort planTrabajoRepository,
        SolicitudNormativaRepositoryPort solicitudRepository,
        DocumentoNormativaRepositoryPort documentoRepository
    ) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.solicitudRepository = Objects.requireNonNull(solicitudRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
    }

    @Override
    public NormativaDetalleDto ejecutar(UUID procesoId) {
        Objects.requireNonNull(procesoId, "El ID del proceso es obligatorio");

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

        List<DocumentoNormativa> documentos = solicitud.getEstado() == EstadoSolicitudNormativa.PENDIENTE_SOLICITUD
            ? Collections.emptyList()
            : documentoRepository.findBySolicitudNormativaId(solicitud.getId());

        return NormativaMapperHelper.toDto(solicitud, documentos, proceso);
    }
}
