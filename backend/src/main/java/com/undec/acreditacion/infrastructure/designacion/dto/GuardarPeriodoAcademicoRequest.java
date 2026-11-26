package com.undec.acreditacion.infrastructure.designacion.dto;

import java.time.LocalDate;

public record GuardarPeriodoAcademicoRequest(
        String codigo,
        String denominacion,
        Integer anio,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Boolean esActual,
        Boolean activo
) {
}
