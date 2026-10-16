package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarPlanTrabajoUseCase;
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
        return planTrabajoRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de trabajo para el proceso: " + procesoId));
    }
}
