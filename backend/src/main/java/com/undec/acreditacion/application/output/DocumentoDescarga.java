package com.undec.acreditacion.application.output;

public record DocumentoDescarga(
        String nombreOriginal,
        String mimeType,
        byte[] bytes
) {
}
