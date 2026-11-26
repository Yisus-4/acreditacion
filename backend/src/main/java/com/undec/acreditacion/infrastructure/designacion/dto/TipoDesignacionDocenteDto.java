package com.undec.acreditacion.infrastructure.designacion.dto;

import java.util.UUID;

public record TipoDesignacionDocenteDto(
        UUID id,
        String codigo,
        String nombre,
        boolean esRentado,
        Integer orden,
        boolean activo
) {
}
