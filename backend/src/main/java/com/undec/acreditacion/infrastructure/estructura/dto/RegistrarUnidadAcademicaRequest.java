package com.undec.acreditacion.infrastructure.estructura.dto;

import java.util.UUID;

public record RegistrarUnidadAcademicaRequest(
        UUID institucionId,
        String nombre,
        String sigla,
        String descripcion
) {
}
