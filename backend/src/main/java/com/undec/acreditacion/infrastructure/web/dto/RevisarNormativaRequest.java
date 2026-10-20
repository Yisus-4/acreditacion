package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.application.input.DocumentoObservadoDto;

import java.util.List;

public record RevisarNormativaRequest(
        String decision,
        String observaciones,
        List<DocumentoObservadoDto> documentosObservados
) {}
