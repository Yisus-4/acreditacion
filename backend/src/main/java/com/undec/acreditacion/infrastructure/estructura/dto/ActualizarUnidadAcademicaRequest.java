package com.undec.acreditacion.infrastructure.estructura.dto;

public record ActualizarUnidadAcademicaRequest(
        String nombre,
        String sigla,
        String descripcion
) {
}
