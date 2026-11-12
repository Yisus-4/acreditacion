package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import java.util.UUID;

public record PlanEstudioVigenteDto(
        UUID id,
        UUID carreraId,
        String carreraNombre,
        String nombrePlan,
        String codigoResolucion,
        Integer anioAprobacion,
        EstadoVigenciaPlan estadoVigencia,
        Boolean esMaestro
) {
}
