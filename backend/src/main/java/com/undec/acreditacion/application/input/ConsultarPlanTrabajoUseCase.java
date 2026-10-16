package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.PlanTrabajo;

import java.util.UUID;

public interface ConsultarPlanTrabajoUseCase {

    PlanTrabajo obtenerPorProcesoId(UUID procesoId);
}
