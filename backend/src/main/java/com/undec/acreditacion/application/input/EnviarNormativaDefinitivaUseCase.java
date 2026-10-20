package com.undec.acreditacion.application.input;

public interface EnviarNormativaDefinitivaUseCase {
    NormativaDetalleDto ejecutar(EnviarNormativaDefinitivaCommand command);
}
