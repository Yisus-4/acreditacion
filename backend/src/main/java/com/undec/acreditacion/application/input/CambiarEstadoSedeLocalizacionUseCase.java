package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.SedeLocalizacion;

import java.util.UUID;

public interface CambiarEstadoSedeLocalizacionUseCase {

    SedeLocalizacion cambiarEstado(UUID id, boolean activo);
}
