package com.undec.acreditacion.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.undec.acreditacion.domain.entities.FaseEvaluacion;

import java.time.LocalDate;
import java.util.List;

public record ElaborarPlanTrabajoRequest(
        String rmEstandares,
        FaseEvaluacion faseEvaluacion,
        @JsonAlias("fechaLimitePresentacionConeau")
        LocalDate fechaLimiteConeau,
        List<ItemActividadPlanRequest> actividades
) {
}
