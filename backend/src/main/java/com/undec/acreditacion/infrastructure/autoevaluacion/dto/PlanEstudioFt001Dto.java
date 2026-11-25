package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlanEstudioFt001Dto(
        UUID id,
        UUID carreraId,
        String carreraNombre,
        UUID procesoId,
        String nombrePlan,
        String codigoResolucion,
        Integer anioAprobacion,
        Integer anioInicio,
        Integer duracionAnios,
        String orientacionesJson,
        String fundamentacionModificaciones,
        String estructuraSintetica,
        Integer horasPresenciales,
        Integer horasNoPresenciales,
        Integer horasTotales,
        Integer horasOptativasMinimas,
        String actividadesComunesCiclo,
        String tituloIntermedioDenominacion,
        String tituloIntermedioDuracion,
        String tituloIntermedioActividades,
        String instanciaFinalGraduacion,
        String otraInformacion,
        Boolean activo,
        List<ActividadCurricularPlanDto> asignaturas,
        Instant creadoEn,
        Instant actualizadoEn,
        UUID estandarId,
        String estandarNombre,
        String estandarResolucion
) {
    public PlanEstudioFt001Dto(
            UUID id,
            UUID carreraId,
            String carreraNombre,
            UUID procesoId,
            String nombrePlan,
            String codigoResolucion,
            Integer anioAprobacion,
            Integer anioInicio,
            Integer duracionAnios,
            String orientacionesJson,
            String fundamentacionModificaciones,
            String estructuraSintetica,
            Integer horasPresenciales,
            Integer horasNoPresenciales,
            Integer horasTotales,
            Integer horasOptativasMinimas,
            String actividadesComunesCiclo,
            String tituloIntermedioDenominacion,
            String tituloIntermedioDuracion,
            String tituloIntermedioActividades,
            String instanciaFinalGraduacion,
            String otraInformacion,
            Boolean activo,
            List<ActividadCurricularPlanDto> asignaturas,
            Instant creadoEn,
            Instant actualizadoEn
    ) {
        this(id, carreraId, carreraNombre, procesoId, nombrePlan, codigoResolucion, anioAprobacion,
                anioInicio, duracionAnios, orientacionesJson, fundamentacionModificaciones,
                estructuraSintetica, horasPresenciales, horasNoPresenciales, horasTotales,
                horasOptativasMinimas, actividadesComunesCiclo, tituloIntermedioDenominacion,
                tituloIntermedioDuracion, tituloIntermedioActividades, instanciaFinalGraduacion,
                otraInformacion, activo, asignaturas, creadoEn, actualizadoEn, null, null, null);
    }
}
