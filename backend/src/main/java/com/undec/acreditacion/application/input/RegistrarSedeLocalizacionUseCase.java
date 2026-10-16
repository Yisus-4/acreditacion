package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.SedeLocalizacion;

public interface RegistrarSedeLocalizacionUseCase {

    SedeLocalizacion registrar(RegistrarSedeLocalizacionCommand command);
}
