package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Institucion;

import java.util.UUID;

public interface ConsultarInstitucionUseCase {

    Institucion consultarPorId(UUID id);
}
