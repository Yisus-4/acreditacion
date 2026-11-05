package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

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
        Boolean esObligatoria
) {
}
