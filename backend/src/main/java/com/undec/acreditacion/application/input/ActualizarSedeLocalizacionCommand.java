package com.undec.acreditacion.application.input;

import java.util.UUID;

public record ActualizarSedeLocalizacionCommand(
        UUID id,
        UUID unidadAcademicaId,
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
    public ActualizarSedeLocalizacionCommand(
            UUID id,
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
        this(id, null, nombre, calle, numero, piso, codigoPostal, provinciaId, departamentoId, localidadId, esPropia);
    }
}
