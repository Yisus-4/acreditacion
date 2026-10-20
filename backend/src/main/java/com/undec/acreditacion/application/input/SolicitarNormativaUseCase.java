package com.undec.acreditacion.application.input;

public interface SolicitarNormativaUseCase {
    NormativaDetalleDto ejecutar(SolicitarNormativaCommand command);
}
