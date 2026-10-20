package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarPlanTrabajoUseCase;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class ConsultarPlanTrabajoService implements ConsultarPlanTrabajoUseCase {

    private final PlanTrabajoRepositoryPort planTrabajoRepository;

    public ConsultarPlanTrabajoService(PlanTrabajoRepositoryPort planTrabajoRepository) {
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
    }

    @Override
    public PlanTrabajo obtenerPorProcesoId(UUID procesoId) {
        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de trabajo para el proceso: " + procesoId));

        boolean tieneActividad13 = plan.getActividades().stream()
                .anyMatch(a -> "1.3".equals(a.getCodigoActividad()) ||
                               (a.getNombre() != null && a.getNombre().toLowerCase().contains("normativa")));

        if (!tieneActividad13) {
            ActividadPlanTrabajo actividad13 = ActividadPlanTrabajo.crearActividadNormativa(plan.getId());
            plan.agregarActividad(actividad13);
            PlanTrabajo saved = planTrabajoRepository.save(plan);
            if (saved != null) {
                plan = saved;
            }
        }

        return plan;
    }
}
