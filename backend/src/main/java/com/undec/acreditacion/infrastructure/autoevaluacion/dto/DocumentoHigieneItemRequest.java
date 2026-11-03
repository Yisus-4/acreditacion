package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.util.UUID;

public record DocumentoHigieneItemRequest(
        UUID id,
        String tipoDocumentoClave,
        String tipoDocumentoNombre,
        String organismoEmisor,
        String numeroNormativa,
        String descripcion,
        String nombreArchivo,
        String urlMinio,
        Boolean obligatorio
) {
}
