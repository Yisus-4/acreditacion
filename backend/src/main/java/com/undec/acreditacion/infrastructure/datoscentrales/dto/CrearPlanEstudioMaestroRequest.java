package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import java.util.UUID;

public record CrearPlanEstudioMaestroRequest(
        UUID carreraId,
        String nombrePlan,
        String codigoResolucion,
        Integer anioAprobacion,
        Integer anioInicio,
        Integer duracionAnios,
        Integer horasPresenciales,
        Integer horasNoPresenciales,
        Integer horasTotales,
        Integer horasOptativasMinimas,
        String orientacionesJson,
        String fundamentacionModificaciones,
        String estructuraSintetica,
        String actividadesComunesCiclo,
        String tituloIntermedioDenominacion,
        String tituloIntermedioDuracion,
        String tituloIntermedioActividades,
        String instanciaFinalGraduacion,
        String otraInformacion
) {
}
