package com.undec.acreditacion.application.input;

import java.util.UUID;

public record EliminarDocumentoBorradorCommand(
    UUID procesoId,
    UUID documentoId,
    UUID usuarioId
) {}
