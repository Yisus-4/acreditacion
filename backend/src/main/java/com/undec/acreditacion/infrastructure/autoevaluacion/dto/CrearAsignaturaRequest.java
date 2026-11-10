package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

public record CrearAsignaturaRequest(
        String codigo,
        String nombre,
        String grupo,
        String subgrupo,
        Integer horasReloj,
        Integer horasPresenciales,
        Integer horasNoPresenciales,
        String correlativas,
        Boolean esObligatoria,
        Integer numeroOrden
) {
}
