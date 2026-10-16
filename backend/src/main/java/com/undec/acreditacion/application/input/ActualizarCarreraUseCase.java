package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Carrera;

public interface ActualizarCarreraUseCase {

    Carrera actualizar(ActualizarCarreraCommand command);
}
