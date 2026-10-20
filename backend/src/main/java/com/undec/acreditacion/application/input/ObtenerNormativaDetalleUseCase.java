package com.undec.acreditacion.application.input;

import java.util.UUID;

public interface ObtenerNormativaDetalleUseCase {
    NormativaDetalleDto ejecutar(UUID procesoId);
}
