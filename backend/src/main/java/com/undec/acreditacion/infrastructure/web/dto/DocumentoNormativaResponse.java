package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentoNormativaResponse(
        UUID id,
        UUID solicitudNormativaId,
        UUID procesoId,
        String tipoDocumento,
        String carpetaDestino,
        String nombreOriginal,
        String storagePath,
        String mimeType,
        long tamanioBytes,
        boolean esBorrador,
        boolean observado,
        String motivoObservacion,
        boolean esObligatorio,
        Instant creadoEn,
        Instant actualizadoEn
) {}
