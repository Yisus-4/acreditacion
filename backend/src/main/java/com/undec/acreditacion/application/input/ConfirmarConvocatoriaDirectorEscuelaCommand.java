package com.undec.acreditacion.application.input;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ConfirmarConvocatoriaDirectorEscuelaCommand(
        UUID convocatoriaId,
        List<UUID> carrerasSeleccionadas,
        UUID confirmadoPorDe,
        Map<UUID, List<UUID>> planesPorCarrera
) {
    public ConfirmarConvocatoriaDirectorEscuelaCommand(UUID convocatoriaId, List<UUID> carrerasSeleccionadas, UUID confirmadoPorDe) {
        this(convocatoriaId, carrerasSeleccionadas, confirmadoPorDe, null);
    }
}
