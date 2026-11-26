package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;

import java.util.UUID;

public record ActualizarAsignaturaRequest(
        UUID id,
        String codigo,
        String nombre,
        String grupo,
        String subgrupo,
        Integer horasReloj,
        Integer horasPresenciales,
        Integer horasNoPresenciales,
        Integer horasSemanales,
        String correlativas,
        Boolean esObligatoria,
        Integer numeroOrden,
        EstadoDictadoActividad estadoDictado
) {
    public ActualizarAsignaturaRequest(
            UUID id,
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
        this(id, codigo, nombre, grupo, subgrupo, horasReloj, horasPresenciales,
                horasNoPresenciales, null, correlativas, esObligatoria, numeroOrden, null);
    }

    public ActualizarAsignaturaRequest(
            UUID id,
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
        this(id, codigo, nombre, grupo, subgrupo, horasReloj, horasPresenciales,
                horasNoPresenciales, null, correlativas, esObligatoria, numeroOrden, estadoDictado);
    }
}
