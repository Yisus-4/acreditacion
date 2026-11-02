package com.undec.acreditacion.infrastructure.web.dto;

import java.time.LocalDate;

public record SolicitarNormativaRequest(
        String observaciones,
        Integer plazoDiasHabiles,
        LocalDate nuevaFechaLimite
) {
    public SolicitarNormativaRequest(String observaciones, Integer plazoDiasHabiles) {
        this(observaciones, plazoDiasHabiles, null);
    }
}
