package com.undec.acreditacion.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record PlanTrabajoResponse(
        UUID id,
        UUID procesoId,
        String codigo,
        String tipo,
        String estado,
        String estadoEnvio,
        List<ActividadDetalleResponse> actividades
) {
    public PlanTrabajoResponse(UUID id,
                               UUID procesoId,
                               String codigo,
                               String tipo,
                               String estado,
                               List<ActividadDetalleResponse> actividades) {
        this(id, procesoId, codigo, tipo, estado, "BORRADOR", actividades);
    }
}
