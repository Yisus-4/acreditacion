package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.RolClaustroCa;

import java.util.UUID;

public record MiembroCaInputDto(
        UUID id,
        String apellido,
        String primerNombre,
        String segundoNombre,
        String dniCuil,
        String correo,
        String telefono,
        RolClaustroCa claustroRol,
        boolean esDocente,
        boolean cuentaConeauGlobalCreada,
        boolean cuentaCvarVinculada,
        int numeroOrden
) {}
