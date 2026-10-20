package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.CarpetaMinioDestino;
import com.undec.acreditacion.domain.entities.TipoDocumentoNormativa;

import java.time.Instant;
import java.util.UUID;

public record DocumentoNormativaDto(
    UUID id,
    UUID solicitudNormativaId,
    UUID procesoId,
    TipoDocumentoNormativa tipoDocumento,
    CarpetaMinioDestino carpetaDestino,
    String nombreOriginal,
    String storagePath,
    String mimeType,
    long tamanioBytes,
    boolean esBorrador,
    boolean observado,
    String motivoObservacion,
    UUID subidoPor,
    Instant creadoEn,
    Instant actualizadoEn
) {}
