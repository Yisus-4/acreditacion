package com.undec.acreditacion.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.undec.acreditacion.domain.entities.FaseEvaluacion;

import java.time.LocalDate;
import java.util.List;

public record ElaborarPlanTrabajoRequest(
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        @JsonAlias("tipoProceso")
        String estadoCarrera,
        String nroResolucionConeau,
        String rmEstandares,
        String areaDisciplinaria,
        String modalidadCarrera,
        FaseEvaluacion faseEvaluacion,
        @JsonAlias("fechaLimitePresentacionConeau")
        LocalDate fechaLimiteConeau,
        List<ItemActividadPlanRequest> actividades
) {
    public ElaborarPlanTrabajoRequest(
            String rmEstandares,
            FaseEvaluacion faseEvaluacion,
            LocalDate fechaLimiteConeau,
            List<ItemActividadPlanRequest> actividades
    ) {
        this(
                null,
                null,
                null,
                null,
                rmEstandares,
                null,
                null,
                faseEvaluacion,
                fechaLimiteConeau,
                actividades
        );
    }
}
