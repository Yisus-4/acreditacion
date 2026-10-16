package com.undec.acreditacion.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record PlanTrabajoResponse(
        UUID id,
        UUID procesoId,
        String codigo,
        String tipo,
        String estado,
        List<ActividadDetalleResponse> actividades
) {
}
