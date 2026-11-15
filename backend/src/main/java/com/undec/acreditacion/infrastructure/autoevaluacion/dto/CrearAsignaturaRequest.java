package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;

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
        Integer numeroOrden,
        EstadoDictadoActividad estadoDictado
) {
    public CrearAsignaturaRequest(
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
        this(codigo, nombre, grupo, subgrupo, horasReloj, horasPresenciales,
                horasNoPresenciales, correlativas, esObligatoria, numeroOrden,
                EstadoDictadoActividad.SE_DICTA);
    }
}
