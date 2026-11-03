package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.util.UUID;

public record GuiaHigieneDocumentoResponse(
        UUID id,
        String tipoDocumentoClave,
        String tipoDocumentoNombre,
        String organismoEmisor,
        String numeroNormativa,
        String descripcion,
        String nombreArchivo,
        String urlMinio,
        String rutaRepositorio,
        Boolean obligatorio,
        Boolean cargado,
        Instant fechaSubida
) {
}
