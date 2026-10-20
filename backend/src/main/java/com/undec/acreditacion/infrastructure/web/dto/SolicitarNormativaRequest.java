package com.undec.acreditacion.infrastructure.web.dto;

public record SolicitarNormativaRequest(
        String observaciones,
        Integer plazoDiasHabiles
) {}
