package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegistroConvocatoriaResponse(
        UUID id,
        String nroResolucionConeau,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        String observaciones,
        String rmEstandares,
        String faseEvaluacion,
        LocalDate fechaLimitePresentacionConeau,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public RegistroConvocatoriaResponse(UUID id,
                                       String nroResolucionConeau,
                                       LocalDate fechaInicio,
                                       LocalDate fechaCierre,
                                       String observaciones,
                                       Instant creadoEn,
                                       Instant actualizadoEn) {
        this(id, nroResolucionConeau, fechaInicio, fechaCierre, observaciones, null, "PRIMERA_FASE", null, creadoEn, actualizadoEn);
    }
}
