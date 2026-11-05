package com.undec.acreditacion.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
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
        List<DocumentoAcreditacionResponse> documentos,
        List<String> areasInformadas,
        List<String> documentacionRemitida,
        String observaciones,
        List<LocalDate> plazosExtendidos,
        List<String> predecesoras,
        boolean bloqueada,
        List<String> predecesorasPendientes
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
                                   String hoja,
                                   LocalDate fechaInicioEstimada,
                                   LocalDate fechaFinEstimada,
                                   String responsable,
                                   String prioridad,
                                   Instant creadoEn,
                                   Instant actualizadoEn,
                                   List<DocumentoAcreditacionResponse> documentos,
                                   List<String> areasInformadas,
                                   List<String> documentacionRemitida,
                                   String observaciones,
                                   List<LocalDate> plazosExtendidos) {
        this(id, planTrabajoId, numeroOrden, codigoActividad, nombre, estado, fechaReunionCoordinacion,
             sintesisAcuerdos, completadaEn, completadaPor, completadaPorNombre, hoja, fechaInicioEstimada,
             fechaFinEstimada, responsable, prioridad, creadoEn, actualizadoEn, documentos, areasInformadas,
             documentacionRemitida, observaciones, plazosExtendidos, Collections.emptyList(), false, Collections.emptyList());
    }

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
                                   String hoja,
                                   LocalDate fechaInicioEstimada,
                                   LocalDate fechaFinEstimada,
                                   String responsable,
                                   String prioridad,
                                   Instant creadoEn,
                                   Instant actualizadoEn,
                                   List<DocumentoAcreditacionResponse> documentos,
                                   List<String> areasInformadas,
                                   List<String> documentacionRemitida,
                                   String observaciones) {
        this(id, planTrabajoId, numeroOrden, codigoActividad, nombre, estado, fechaReunionCoordinacion,
             sintesisAcuerdos, completadaEn, completadaPor, completadaPorNombre, hoja, fechaInicioEstimada,
             fechaFinEstimada, responsable, prioridad, creadoEn, actualizadoEn, documentos, areasInformadas,
             documentacionRemitida, observaciones, Collections.emptyList());
    }

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
             creadoEn, actualizadoEn, documentos, Collections.emptyList(), Collections.emptyList(), sintesisAcuerdos, Collections.emptyList());
    }
}
