package com.undec.acreditacion.infrastructure.designacion.dto;

import java.time.Instant;
import java.util.UUID;

public record DepartamentoAcademicoDto(
        UUID id,
        UUID institucionId,
        String codigo,
        String denominacion,
        String descripcion,
        boolean activo,
        boolean esSistema,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
