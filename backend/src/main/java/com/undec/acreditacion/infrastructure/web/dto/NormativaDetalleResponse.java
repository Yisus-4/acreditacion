package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.application.input.BadgePlazoDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NormativaDetalleResponse(
        UUID id,
        UUID procesoId,
        UUID actividadId,
        String estado,
        Instant fechaSolicitud,
        LocalDate fechaLimiteEntrega,
        int diasHabilesPlazo,
        BadgePlazoDto badgePlazo,
        double porcentajeAvance,
        int documentosCargados,
        int documentosRequeridos,
        String observacionesSolicitud,
        String observacionesRevision,
        List<DocumentoNormativaResponse> documentos,
        List<String> documentosFaltantes,
        boolean carreraEsNueva,
        boolean carreraTieneHistorial
) {}
