package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Carrera;

import java.util.UUID;

public interface CambiarEstadoCarreraUseCase {

    Carrera cambiarEstado(UUID id, boolean activo);
}
