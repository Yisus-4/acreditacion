package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.RolClaustroCa;

import java.util.UUID;

public record MiembroCaInputDto(
        UUID id,
        String apellido,
        String primerNombre,
        String segundoNombre,
        String nombreCompleto,
        String dniCuil,
        String documento,
        String cuitPasaporte,
        String correo,
        String correoGmail,
        String cargoFuncion,
        String telefono,
        RolClaustroCa claustroRol,
        boolean pendienteAltaSistema,
        int numeroOrden
) {
    public MiembroCaInputDto(
            UUID id,
            String apellido,
            String primerNombre,
            String segundoNombre,
            String dniCuil,
            String correo,
            String telefono,
            RolClaustroCa claustroRol,
            int numeroOrden
    ) {
        this(
                id,
                apellido,
                primerNombre,
                segundoNombre,
                primerNombre + (segundoNombre != null && !segundoNombre.isBlank() ? " " + segundoNombre : ""),
                dniCuil,
                dniCuil,
                null,
                correo,
                null,
                null,
                telefono,
                claustroRol,
                false,
                numeroOrden
        );
    }

    public String resolverDocumento() {
        if (documento != null && !documento.isBlank()) return documento.trim();
        if (dniCuil != null && !dniCuil.isBlank()) return dniCuil.trim();
        return "";
    }

    public String resolverNombreCompleto() {
        if (nombreCompleto != null && !nombreCompleto.isBlank()) return nombreCompleto.trim();
        if (primerNombre != null && !primerNombre.isBlank()) {
            if (segundoNombre != null && !segundoNombre.isBlank()) {
                return primerNombre.trim() + " " + segundoNombre.trim();
            }
            return primerNombre.trim();
        }
        return "";
    }
}
