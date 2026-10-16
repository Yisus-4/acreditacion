package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.UnidadAcademica;

import java.util.UUID;

public interface CambiarEstadoUnidadAcademicaUseCase {

    UnidadAcademica cambiarEstado(UUID id, boolean activo);
}
