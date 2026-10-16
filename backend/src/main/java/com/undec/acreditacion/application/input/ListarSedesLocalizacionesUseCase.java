package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.SedeLocalizacion;

import java.util.List;
import java.util.UUID;

public interface ListarSedesLocalizacionesUseCase {

    List<SedeLocalizacion> listarTodas();

    List<SedeLocalizacion> listarPorInstitucion(UUID institucionId);
}
