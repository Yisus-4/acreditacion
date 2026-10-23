package com.undec.acreditacion.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record ConfirmarConvocatoriaDeRequest(
        List<UUID> carrerasSeleccionadas
) {
}
