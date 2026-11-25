package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;

import java.util.UUID;

public record ActividadCurricularPlanDto(
        UUID id,
        UUID planEstudioId,
        Integer numeroOrden,
        String codigo,
        String nombre,
        String grupo,
        String subgrupo,
        Integer horasReloj,
        Integer horasPresenciales,
        Integer horasNoPresenciales,
        String correlativas,
        Boolean esObligatoria,
        EstadoDictadoActividad estadoDictado,
        UUID docenteResponsableId,
        String docenteResponsableNombre,
        boolean seccion22Completa,
        boolean seccion3Completa
) {
    public ActividadCurricularPlanDto(
            UUID id,
            UUID planEstudioId,
            Integer numeroOrden,
            String codigo,
            String nombre,
            String grupo,
            String subgrupo,
            Integer horasReloj,
            Integer horasPresenciales,
            Integer horasNoPresenciales,
            String correlativas,
            Boolean esObligatoria
    ) {
        this(id, planEstudioId, numeroOrden, codigo, nombre, grupo, subgrupo,
                horasReloj, horasPresenciales, horasNoPresenciales, correlativas,
                esObligatoria, EstadoDictadoActividad.SE_DICTA, null, null, false, false);
    }

    public ActividadCurricularPlanDto(
            UUID id,
            UUID planEstudioId,
            Integer numeroOrden,
            String codigo,
            String nombre,
            String grupo,
            String subgrupo,
            Integer horasReloj,
            Integer horasPresenciales,
            Integer horasNoPresenciales,
            String correlativas,
            Boolean esObligatoria,
            EstadoDictadoActividad estadoDictado
    ) {
        this(id, planEstudioId, numeroOrden, codigo, nombre, grupo, subgrupo,
                horasReloj, horasPresenciales, horasNoPresenciales, correlativas,
                esObligatoria, estadoDictado, null, null, false, false);
    }
}
