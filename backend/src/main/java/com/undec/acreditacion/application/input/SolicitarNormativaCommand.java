package com.undec.acreditacion.application.input;

import java.util.UUID;

public record SolicitarNormativaCommand(
    UUID procesoId,
    UUID solicitanteId,
    String observaciones
) {}
