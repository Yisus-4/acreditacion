package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ConvocatoriaResumenResponse(
        UUID id,
        String codigo,
        String titulo,
        UUID carreraId,
        String carreraNombre,
        String carreraSigla,
        String nivel,
        String nroResolucionConeau,
        String tipoProceso,
        String estado,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        String planTrabajoCodigo,
        Instant creadoEn
) {
}
