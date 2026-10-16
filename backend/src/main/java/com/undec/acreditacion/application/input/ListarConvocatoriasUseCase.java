package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;

import java.util.List;

public interface ListarConvocatoriasUseCase {

    List<ProcesoAcreditacion> listar(ConvocatoriaFiltro filtro);
}
