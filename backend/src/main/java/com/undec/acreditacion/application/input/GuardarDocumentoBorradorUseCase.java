package com.undec.acreditacion.application.input;

public interface GuardarDocumentoBorradorUseCase {
    DocumentoNormativaDto ejecutar(GuardarDocumentoBorradorCommand command);
}
