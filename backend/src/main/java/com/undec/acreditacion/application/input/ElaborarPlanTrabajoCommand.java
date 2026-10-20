package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.FaseEvaluacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ElaborarPlanTrabajoCommand(
        UUID procesoId,
        String rmEstandares,
        FaseEvaluacion faseEvaluacion,
        LocalDate fechaLimiteConeau,
        List<ItemActividadPlanCommand> actividades,
        UUID usuarioAeaId
) {
}
