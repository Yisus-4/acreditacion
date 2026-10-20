package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.PlanTrabajo;

public interface ElaborarPlanTrabajoUseCase {
    PlanTrabajo elaborar(ElaborarPlanTrabajoCommand command);
}
