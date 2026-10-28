package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.Convocatoria;

import java.util.UUID;

public interface ReiniciarConvocatoriaEscuelaTestUseCase {
    Convocatoria reiniciar(UUID convocatoriaId);
}
