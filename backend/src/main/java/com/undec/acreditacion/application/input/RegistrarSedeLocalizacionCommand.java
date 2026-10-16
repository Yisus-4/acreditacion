package com.undec.acreditacion.application.input;

import java.util.UUID;

public record RegistrarSedeLocalizacionCommand(
        UUID institucionId,
        String nombre,
        String calle,
        String numero,
        String piso,
        String codigoPostal,
        Long provinciaId,
        Long departamentoId,
        Long localidadId,
        boolean esPropia
) {
}
