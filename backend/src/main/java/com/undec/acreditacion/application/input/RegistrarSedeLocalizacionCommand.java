package com.undec.acreditacion.application.input;

import java.util.UUID;

public record RegistrarSedeLocalizacionCommand(
        UUID institucionId,
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
    public RegistrarSedeLocalizacionCommand(
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
        this(institucionId, null, nombre, calle, numero, piso, codigoPostal, provinciaId, departamentoId, localidadId, esPropia);
    }
}
