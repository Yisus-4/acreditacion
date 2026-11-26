package com.undec.acreditacion.infrastructure.designacion.dto;

public record GuardarCargoDocenteRequest(
        String codigo,
        String nombre,
        Integer orden,
        Boolean activo
) {
}
