package com.undec.acreditacion.domain.exceptions;

import com.undec.acreditacion.domain.entities.TipoDocumentoNormativa;
import java.util.Collections;
import java.util.List;

public class DocumentosFaltantesException extends DomainValidationException {

    private final List<TipoDocumentoNormativa> documentosFaltantes;

    public DocumentosFaltantesException(List<TipoDocumentoNormativa> documentosFaltantes) {
        super("Faltan documentos obligatorios para la tipología de la carrera: " + documentosFaltantes);
        this.documentosFaltantes = documentosFaltantes != null 
            ? List.copyOf(documentosFaltantes) 
            : Collections.emptyList();
    }

    public List<TipoDocumentoNormativa> getDocumentosFaltantes() {
        return documentosFaltantes;
    }
}
