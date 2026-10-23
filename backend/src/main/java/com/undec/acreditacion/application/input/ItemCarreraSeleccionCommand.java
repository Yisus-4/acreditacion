package com.undec.acreditacion.application.input;

import java.util.UUID;

public record ItemCarreraSeleccionCommand(
        UUID carreraId,
        boolean seleccionada,
        boolean esObligatoria
) {
}
