package com.undec.acreditacion.infrastructure.web.dto;

import java.util.UUID;

public record PlanTrabajoResumenResponse(
        UUID id,
        String codigo,
        String tipo,
        String estado,
        String estadoEnvio,
        int actividadesCount
) {
    public PlanTrabajoResumenResponse(UUID id,
                                      String codigo,
                                      String tipo,
                                      String estado,
                                      int actividadesCount) {
        this(id, codigo, tipo, estado, "BORRADOR", actividadesCount);
    }
}
