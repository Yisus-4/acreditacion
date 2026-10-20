package com.undec.acreditacion.application.input;

public interface RevisarNormativaUseCase {
    NormativaDetalleDto ejecutar(RevisarNormativaCommand command);
}
