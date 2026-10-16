package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Carrera;

public interface RegistrarCarreraUseCase {

    Carrera registrar(RegistrarCarreraCommand command);
}
