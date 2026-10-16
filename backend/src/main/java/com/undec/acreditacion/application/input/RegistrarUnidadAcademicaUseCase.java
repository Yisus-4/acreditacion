package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.UnidadAcademica;

public interface RegistrarUnidadAcademicaUseCase {

    UnidadAcademica registrar(RegistrarUnidadAcademicaCommand command);
}
