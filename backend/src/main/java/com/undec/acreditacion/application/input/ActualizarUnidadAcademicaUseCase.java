package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.UnidadAcademica;

public interface ActualizarUnidadAcademicaUseCase {

    UnidadAcademica actualizar(ActualizarUnidadAcademicaCommand command);
}
