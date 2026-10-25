package com.undec.acreditacion.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

public record GuardarCronogramaPlanRequest(
        LocalDate fechaLimiteConeau,
        List<ItemActividadPlanRequest> actividades
) {
}
