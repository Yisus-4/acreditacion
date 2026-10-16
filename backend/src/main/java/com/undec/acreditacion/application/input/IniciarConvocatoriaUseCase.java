package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;

public interface IniciarConvocatoriaUseCase {

    ProcesoAcreditacion iniciar(IniciarConvocatoriaCommand command);
}
