package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.TipoDocumentoNormativa;

import java.util.List;

public record DocumentosFaltantesDto(
    String codigo,
    String mensaje,
    List<TipoDocumentoNormativa> documentosFaltantes
) {}
