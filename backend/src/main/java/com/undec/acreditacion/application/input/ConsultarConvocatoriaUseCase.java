package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;

import java.util.UUID;

public interface ConsultarConvocatoriaUseCase {

    ProcesoAcreditacion obtenerPorId(UUID id);
}
