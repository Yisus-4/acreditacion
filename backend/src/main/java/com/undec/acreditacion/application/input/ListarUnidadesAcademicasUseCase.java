package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.UnidadAcademica;

import java.util.List;
import java.util.UUID;

public interface ListarUnidadesAcademicasUseCase {

    List<UnidadAcademica> listarTodas();

    List<UnidadAcademica> listarPorInstitucion(UUID institucionId);
}
