package com.undec.acreditacion.infrastructure.web.dto;

import java.util.UUID;

public record CarreraSeleccionItemRequest(
        UUID carreraId,
        Boolean seleccionada,
        Boolean esObligatoria
) {
}
