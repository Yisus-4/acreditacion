package com.undec.acreditacion.application.input;

import java.util.UUID;

public record DocumentoObservadoDto(
    UUID documentoId,
    String motivo
) {}
