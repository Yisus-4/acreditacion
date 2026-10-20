package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.DecisionRevisionNormativa;

import java.util.List;
import java.util.UUID;

public record RevisarNormativaCommand(
    UUID procesoId,
    UUID revisorId,
    DecisionRevisionNormativa decision,
    String observacionesRevision,
    List<DocumentoObservadoDto> documentosObservados
) {}
