package com.undec.acreditacion.infrastructure.designacion.dto;

public record GuardarDepartamentoAcademicoRequest(
        String codigo,
        String denominacion,
        String descripcion,
        Boolean activo
) {
}
