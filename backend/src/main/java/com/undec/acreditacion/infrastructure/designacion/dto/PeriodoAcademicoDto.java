package com.undec.acreditacion.infrastructure.designacion.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PeriodoAcademicoDto(
        UUID id,
        String codigo,
        String denominacion,
        Integer anio,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean esActual,
        boolean activo
) {
}
