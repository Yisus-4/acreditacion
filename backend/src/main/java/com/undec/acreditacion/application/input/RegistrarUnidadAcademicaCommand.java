package com.undec.acreditacion.application.input;

import java.util.UUID;

public record RegistrarUnidadAcademicaCommand(
        UUID institucionId,
        String nombre,
        String sigla,
        String descripcion
) {
}
