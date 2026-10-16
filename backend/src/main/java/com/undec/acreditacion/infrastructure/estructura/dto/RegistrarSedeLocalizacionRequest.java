package com.undec.acreditacion.infrastructure.estructura.dto;

import java.util.UUID;

public record RegistrarSedeLocalizacionRequest(
        UUID institucionId,
        String nombre,
        String calle,
        String numero,
        String piso,
        String codigoPostal,
        Long provinciaId,
        Long departamentoId,
        Long localidadId,
        Boolean esPropia
) {
}
