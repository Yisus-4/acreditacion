package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentoAcreditacionResponse(
        UUID id,
        String nombreOriginal,
        String tipoDocumento,
        String mimeType,
        long tamanioBytes,
        String descargaUrl,
        Instant subidoEn
) {
}
