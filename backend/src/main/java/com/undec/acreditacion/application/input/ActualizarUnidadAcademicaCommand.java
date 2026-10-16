package com.undec.acreditacion.application.input;

import java.util.UUID;

public record ActualizarUnidadAcademicaCommand(
        UUID id,
        String nombre,
        String sigla,
        String descripcion
) {
}
