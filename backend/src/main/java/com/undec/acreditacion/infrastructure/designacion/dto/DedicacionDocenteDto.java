package com.undec.acreditacion.infrastructure.designacion.dto;

import java.util.UUID;

public record DedicacionDocenteDto(
        UUID id,
        String codigo,
        String nombre,
        Integer horasSemanales,
        Integer orden,
        boolean activo
) {
}
