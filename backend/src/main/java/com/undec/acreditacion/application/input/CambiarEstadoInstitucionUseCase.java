package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Institucion;

import java.util.UUID;

public interface CambiarEstadoInstitucionUseCase {

    Institucion cambiarEstado(UUID id, boolean activo);
}
