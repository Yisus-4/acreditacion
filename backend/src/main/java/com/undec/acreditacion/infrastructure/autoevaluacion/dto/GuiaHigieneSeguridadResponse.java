package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GuiaHigieneSeguridadResponse(
        UUID id,
        UUID procesoId,
        Boolean esMaestro,
        String edicion,
        LocalDate fechaVigencia,
        String elaboradoPor,
        String revisadoPor,
        String aprobadoPor,
        String instanciaResponsableUa,
        String politicaUnidadesApoyo,
        Boolean aplicaUnidadesApoyo,
        Boolean verificado,
        Instant fechaVerificacion,
        Instant fechaSolicitud,
        LocalDate fechaLimite,
        String estado,
        String observaciones,
        List<GuiaHigieneDocumentoResponse> documentos,
        int cantidadDocumentosCargados,
        int totalObligatorios,
        boolean requisitosCompletos
) {
}
