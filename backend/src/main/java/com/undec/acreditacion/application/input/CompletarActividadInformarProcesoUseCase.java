package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;

public interface CompletarActividadInformarProcesoUseCase {

    ActividadPlanTrabajo completar(CompletarActividadCommand command);
}
