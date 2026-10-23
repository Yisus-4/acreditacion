package com.undec.acreditacion.application.input;

import java.util.List;
import java.util.UUID;

public record ConfirmarConvocatoriaDirectorEscuelaCommand(
        UUID convocatoriaId,
        List<UUID> carrerasSeleccionadas,
        UUID confirmadoPorDe
) {
}
