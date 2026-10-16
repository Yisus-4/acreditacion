package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActividadDetalleResponse(
        UUID id,
        UUID planTrabajoId,
        int numeroOrden,
        String codigoActividad,
        String nombre,
        String estado,
        Instant fechaReunionCoordinacion,
        String sintesisAcuerdos,
        Instant completadaEn,
        UUID completadaPor,
        String completadaPorNombre,
        Instant creadoEn,
        Instant actualizadoEn,
        List<DocumentoAcreditacionResponse> documentos
) {
}
