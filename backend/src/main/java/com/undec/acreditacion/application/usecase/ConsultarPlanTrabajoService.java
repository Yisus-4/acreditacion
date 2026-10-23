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

        boolean modificado = false;

        boolean tieneActividad13 = plan.getActividades().stream().anyMatch(PlanTrabajo::esActividadNormativa);
        if (!tieneActividad13) {
            ActividadPlanTrabajo actividad13 = ActividadPlanTrabajo.crearActividadNormativa(plan.getId());
            plan.agregarActividad(actividad13);
            persistirActividadAgregada(plan, actividad13.getId());
            modificado = true;
        }

        boolean tieneActividad04 = plan.getActividades().stream().anyMatch(PlanTrabajo::esActividadIdentificarDocumentacionInteres);
        if (!tieneActividad04) {
            ActividadPlanTrabajo actividad04 = ActividadPlanTrabajo.crearActividadIdentificarDocumentacionInteres(plan.getId());
            plan.agregarActividad(actividad04);
            persistirActividadAgregada(plan, actividad04.getId());
            modificado = true;
        }

        // Eliminar Actividad 5 obsoleta de planes existentes
        boolean teniaActividad05 = plan.eliminarActividadSiExiste("ACT-05-VERIFICAR-INFO-INSTITUCIONAL")
                || plan.eliminarActividadSiExiste("1.7")
                || plan.eliminarActividadSiExiste("información institucional precargada");
        if (teniaActividad05) {
            modificado = true;
        }

        boolean tieneActividad06 = plan.getActividades().stream().anyMatch(PlanTrabajo::esActividadFormalizacionElectronica);
        if (!tieneActividad06) {
            ActividadPlanTrabajo actividad06 = ActividadPlanTrabajo.crearActividadFormalizacionElectronica(plan.getId());
            plan.agregarActividad(actividad06);
            persistirActividadAgregada(plan, actividad06.getId());
            modificado = true;
        }

        if (modificado) {
            PlanTrabajo saved = planTrabajoRepository.save(plan);
            if (saved != null) {
                plan = saved;
            }
        }

        return plan;
    }

    private void persistirActividadAgregada(PlanTrabajo plan, UUID actividadId) {
        plan.getActividades().stream()
                .filter(a -> a.getId().equals(actividadId))
                .findFirst()
                .ifPresent(planTrabajoRepository::saveActividad);
    }
}
