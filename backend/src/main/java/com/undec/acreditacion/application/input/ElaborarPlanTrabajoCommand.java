package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.FaseEvaluacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ElaborarPlanTrabajoCommand(
        UUID procesoId,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        String estadoCarrera,
        String nroResolucionConeau,
        String rmEstandares,
        String areaDisciplinaria,
        String modalidadCarrera,
        FaseEvaluacion faseEvaluacion,
        LocalDate fechaLimiteConeau,
        List<ItemActividadPlanCommand> actividades,
        UUID usuarioAeaId
) {
    public ElaborarPlanTrabajoCommand(
            UUID procesoId,
            String rmEstandares,
            FaseEvaluacion faseEvaluacion,
            LocalDate fechaLimiteConeau,
            List<ItemActividadPlanCommand> actividades,
            UUID usuarioAeaId
    ) {
        this(
                procesoId,
                null,
                null,
                null,
                null,
                rmEstandares,
                null,
                null,
                faseEvaluacion,
                fechaLimiteConeau,
                actividades,
                usuarioAeaId
        );
    }
}
