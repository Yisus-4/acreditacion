package com.undec.acreditacion.infrastructure.estructura.dto;

public record ActualizarSedeLocalizacionRequest(
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
