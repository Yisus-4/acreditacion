package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.domain.entities.FaseEvaluacion;

import java.time.LocalDate;
import java.util.List;

public record ElaborarPlanTrabajoRequest(
        String rmEstandares,
        FaseEvaluacion faseEvaluacion,
        LocalDate fechaLimiteConeau,
        List<ItemActividadPlanRequest> actividades
) {
}
