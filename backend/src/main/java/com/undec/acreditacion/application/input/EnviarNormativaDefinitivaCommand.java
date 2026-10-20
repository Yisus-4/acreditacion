package com.undec.acreditacion.application.input;

import java.util.UUID;

public record EnviarNormativaDefinitivaCommand(
    UUID procesoId,
    UUID usuarioId
) {}
