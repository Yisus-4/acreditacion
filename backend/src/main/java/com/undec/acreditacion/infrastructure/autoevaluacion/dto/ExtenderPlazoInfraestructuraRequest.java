package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.LocalDate;

public record ExtenderPlazoInfraestructuraRequest(
        LocalDate nuevaFechaLimite,
        String motivo
) {
}
