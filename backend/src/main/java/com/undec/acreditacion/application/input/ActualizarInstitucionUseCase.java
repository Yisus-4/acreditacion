package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Institucion;

import java.util.UUID;

public interface ActualizarInstitucionUseCase {

    Institucion actualizar(UUID id, RegistrarInstitucionCommand command);
}
