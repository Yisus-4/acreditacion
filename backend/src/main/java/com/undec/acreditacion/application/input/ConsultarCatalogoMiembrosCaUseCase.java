package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.CatalogoMiembroComisionDto;

import java.util.List;

public interface ConsultarCatalogoMiembrosCaUseCase {

    List<CatalogoMiembroComisionDto> buscar(String busqueda);
}
