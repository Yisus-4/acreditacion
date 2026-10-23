package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentoAcreditacionResponse(
        UUID id,
        String nombreOriginal,
        String tipoDocumento,
        String clasificacionOrigen,
        String descripcion,
        String mimeType,
        long tamanioBytes,
        String descargaUrl,
        Instant subidoEn
) {
    public DocumentoAcreditacionResponse(
            UUID id,
            String nombreOriginal,
            String tipoDocumento,
            String mimeType,
            long tamanioBytes,
            String descargaUrl,
            Instant subidoEn
    ) {
        this(
                id,
                nombreOriginal,
                tipoDocumento,
                null,
                null,
                mimeType,
                tamanioBytes,
                descargaUrl,
                subidoEn
        );
    }
}
