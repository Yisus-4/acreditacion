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
        Instant creadoEn,
        Instant actualizadoEn
) {
}
