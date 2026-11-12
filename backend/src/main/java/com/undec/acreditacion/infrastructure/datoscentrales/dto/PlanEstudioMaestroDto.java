package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import java.time.Instant;
import java.util.UUID;

public record PlanEstudioMaestroDto(
        UUID id,
        UUID carreraId,
        String carreraNombre,
        String carreraSigla,
        String nombrePlan,
        EstadoVigenciaPlan estadoVigencia,
        Boolean esMaestro,
        String codigoResolucion,
        Integer anioAprobacion,
        Integer anioInicio,
        Integer duracionAnios,
        Integer horasPresenciales,
        Integer horasNoPresenciales,
        Integer horasTotales,
        Integer horasOptativasMinimas,
        Boolean activo,
        int cantidadAsignaturas,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
