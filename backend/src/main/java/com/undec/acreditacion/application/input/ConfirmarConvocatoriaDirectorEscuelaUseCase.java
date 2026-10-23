package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Convocatoria;

public interface ConfirmarConvocatoriaDirectorEscuelaUseCase {
    Convocatoria confirmar(ConfirmarConvocatoriaDirectorEscuelaCommand command);
}
