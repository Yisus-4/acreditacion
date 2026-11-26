package com.undec.acreditacion.infrastructure.designacion.dto;

import java.util.UUID;

public record CargoDocenteDto(
        UUID id,
        String codigo,
        String nombre,
        Integer orden,
        boolean activo
) {
}
