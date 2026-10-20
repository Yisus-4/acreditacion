package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
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
        String hoja,
        LocalDate fechaInicioEstimada,
        LocalDate fechaFinEstimada,
        String responsable,
        String prioridad,
        Instant creadoEn,
        Instant actualizadoEn,
        List<DocumentoAcreditacionResponse> documentos
) {
    public ActividadDetalleResponse(UUID id,
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
                                   List<DocumentoAcreditacionResponse> documentos) {
        this(id, planTrabajoId, numeroOrden, codigoActividad, nombre, estado, fechaReunionCoordinacion,
             sintesisAcuerdos, completadaEn, completadaPor, completadaPorNombre, "INFO", null, null, null, "MEDIA",
             creadoEn, actualizadoEn, documentos);
    }
}
