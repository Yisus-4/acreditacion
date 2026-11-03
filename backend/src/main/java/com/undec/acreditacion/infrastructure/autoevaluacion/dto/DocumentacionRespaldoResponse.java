package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentacionRespaldoResponse(
        UUID id,
        String nombreArchivo,
        String urlMinio,
        String rutaRepositorio,
        String tipoAnexo,
        String descripcion,
        String subidoPorUsuarioNombre,
        Instant fechaSubida
) {
}
