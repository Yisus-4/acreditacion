package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.TipoDocumentoNormativa;

import java.util.UUID;

public record GuardarDocumentoBorradorCommand(
    UUID procesoId,
    TipoDocumentoNormativa tipoDocumento,
    String nombreOriginal,
    byte[] contenido,
    String mimeType,
    UUID subidoPor
) {}
