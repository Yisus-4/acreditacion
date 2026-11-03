package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.util.UUID;

public record VerificarInfraestructuraResponse(
        UUID procesoId,
        boolean verificado,
        String estadoActividadPlan,
        Instant fechaVerificacion,
        String mensaje
) {
}
