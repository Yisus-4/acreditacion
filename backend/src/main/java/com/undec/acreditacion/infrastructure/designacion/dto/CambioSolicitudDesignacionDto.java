package com.undec.acreditacion.infrastructure.designacion.dto;

import com.undec.acreditacion.domain.entities.TipoEventoSolicitudDesignacion;

import java.time.Instant;
import java.util.UUID;

public record CambioSolicitudDesignacionDto(
        UUID id,
        UUID solicitudId,
        UUID designacionId,
        TipoEventoSolicitudDesignacion tipoEvento,
        String campoModificado,
        String valorAnterior,
        String valorNuevo,
        String motivo,
        UUID usuarioId,
        String usuarioNombre,
        Instant fechaCambio
) {}
