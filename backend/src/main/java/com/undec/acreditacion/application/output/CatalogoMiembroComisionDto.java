package com.undec.acreditacion.application.output;

import java.time.Instant;
import java.util.UUID;

public record CatalogoMiembroComisionDto(
        UUID id,
        String apellido,
        String nombreCompleto,
        String documento,
        String cuitPasaporte,
        String correoElectronico,
        String correoGmail,
        String cargoFuncion,
        boolean esDocente,
        Instant creadoEn
) {}
