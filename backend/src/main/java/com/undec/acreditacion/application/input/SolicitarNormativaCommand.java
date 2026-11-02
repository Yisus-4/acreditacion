package com.undec.acreditacion.application.input;

import java.time.LocalDate;
import java.util.UUID;

public record SolicitarNormativaCommand(
    UUID procesoId,
    UUID solicitanteId,
    String observaciones,
    Integer plazoDiasHabiles,
    LocalDate nuevaFechaLimite
) {
    public SolicitarNormativaCommand(UUID procesoId, UUID solicitanteId, String observaciones, Integer plazoDiasHabiles) {
        this(procesoId, solicitanteId, observaciones, plazoDiasHabiles, null);
    }

    public SolicitarNormativaCommand(UUID procesoId, UUID solicitanteId, String observaciones) {
        this(procesoId, solicitanteId, observaciones, 5, null);
    }
}
