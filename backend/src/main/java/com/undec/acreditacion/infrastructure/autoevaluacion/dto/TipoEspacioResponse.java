package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.util.UUID;

public record TipoEspacioResponse(
        UUID id,
        String codigo,
        String denominacion,
        Boolean activo
) {
}
