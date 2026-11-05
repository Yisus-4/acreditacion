package com.undec.acreditacion.infrastructure.estructura.dto;

import java.util.UUID;

public record ActualizarSedeLocalizacionRequest(
        UUID unidadAcademicaId,
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
    public ActualizarSedeLocalizacionRequest(
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
        this(null, nombre, calle, numero, piso, codigoPostal, provinciaId, departamentoId, localidadId, esPropia);
    }
}
