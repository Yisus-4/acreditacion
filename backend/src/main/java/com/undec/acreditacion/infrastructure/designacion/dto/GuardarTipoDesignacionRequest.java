package com.undec.acreditacion.infrastructure.designacion.dto;

public record GuardarTipoDesignacionRequest(
        String codigo,
        String nombre,
        Boolean esRentado,
        Integer orden,
        Boolean activo
) {
}
