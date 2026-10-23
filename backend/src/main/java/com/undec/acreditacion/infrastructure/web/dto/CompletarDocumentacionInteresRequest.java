package com.undec.acreditacion.infrastructure.web.dto;

import java.util.List;

public record CompletarDocumentacionInteresRequest(
        List<String> fuentesSeleccionadas,
        String observaciones
) {
}
