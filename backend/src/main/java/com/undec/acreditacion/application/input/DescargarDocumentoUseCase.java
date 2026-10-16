package com.undec.acreditacion.application.input;

import com.undec.acreditacion.application.output.DocumentoDescarga;

import java.util.UUID;

public interface DescargarDocumentoUseCase {

    DocumentoDescarga descargar(UUID documentoId);
}
