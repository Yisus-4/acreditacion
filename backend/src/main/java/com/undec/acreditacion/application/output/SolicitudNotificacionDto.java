package com.undec.acreditacion.application.output;

import java.time.Instant;
import java.util.UUID;

public record SolicitudNotificacionDto(
        UUID id,
        String tipo,
        String titulo,
        String mensaje,
        String datosPayload,
        UUID procesoId,
        String carreraNombre,
        UUID solicitadoPor,
        String solicitadoPorNombre,
        String destinatarioRol,
        String estado,
        boolean leida,
        UUID resueltoPor,
        Instant resueltoEn,
        Instant creadoEn
) {}
