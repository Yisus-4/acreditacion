package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.SolicitudNotificacionDto;

import java.util.UUID;

public interface GestionarSolicitudNotificacionUseCase {
    SolicitudNotificacionDto aceptar(UUID id, UUID resolutorId);
    SolicitudNotificacionDto rechazar(UUID id, UUID resolutorId);
    SolicitudNotificacionDto marcarLeida(UUID id);
}
