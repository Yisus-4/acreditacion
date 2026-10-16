package com.undec.acreditacion.infrastructure.web.dto;

import java.util.UUID;

public record PlanTrabajoResumenResponse(
        UUID id,
        String codigo,
        String tipo,
        String estado,
        int actividadesCount
) {
}
