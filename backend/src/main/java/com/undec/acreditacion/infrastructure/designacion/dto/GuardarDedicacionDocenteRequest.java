package com.undec.acreditacion.infrastructure.designacion.dto;

public record GuardarDedicacionDocenteRequest(
        String codigo,
        String nombre,
        Integer horasSemanales,
        Integer orden,
        Boolean activo
) {
}
