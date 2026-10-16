package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Carrera;

import java.util.List;
import java.util.UUID;

public interface ListarCarrerasUseCase {

    List<Carrera> listarTodas();

    List<Carrera> listarPorUnidadAcademica(UUID unidadAcademicaId);
}
