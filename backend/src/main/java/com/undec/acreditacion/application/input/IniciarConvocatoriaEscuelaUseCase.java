package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Convocatoria;

public interface IniciarConvocatoriaEscuelaUseCase {
    Convocatoria iniciar(IniciarConvocatoriaEscuelaCommand command);
}
