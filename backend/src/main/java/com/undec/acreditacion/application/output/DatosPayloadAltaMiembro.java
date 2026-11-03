package com.undec.acreditacion.application.output;

public record DatosPayloadAltaMiembro(
        String apellido,
        String nombreCompleto,
        String documento,
        String cuitPasaporte,
        String correoElectronico,
        String correoGmail,
        String cargoFuncion
) {}
