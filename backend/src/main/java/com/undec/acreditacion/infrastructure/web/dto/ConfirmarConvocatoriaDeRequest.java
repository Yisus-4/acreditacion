package com.undec.acreditacion.infrastructure.web.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConfirmarConvocatoriaDeRequest(
        List<UUID> carrerasSeleccionadas,
        Map<UUID, List<UUID>> planesPorCarrera
) {
    public ConfirmarConvocatoriaDeRequest(List<UUID> carrerasSeleccionadas) {
        this(carrerasSeleccionadas, null);
    }
}
