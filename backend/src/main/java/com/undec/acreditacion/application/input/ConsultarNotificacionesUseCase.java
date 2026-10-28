package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.SolicitudNotificacionDto;

import java.util.List;

public interface ConsultarNotificacionesUseCase {
    List<SolicitudNotificacionDto> listar(String rol, String estado);
    long contarPendientes(String rol);
}
