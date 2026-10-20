package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.CumplimientoPlazo;
import com.undec.acreditacion.domain.entities.EstadoSolicitudNormativa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NormativaDetalleDto(
    UUID id,
    UUID procesoId,
    UUID actividadId,
    EstadoSolicitudNormativa estado,
    Instant fechaSolicitud,
    UUID solicitadoPor,
    LocalDate fechaLimiteEntrega,
    int diasHabilesPlazo,
    Instant fechaEnvio,
    UUID enviadoPor,
    CumplimientoPlazo cumplimientoPlazo,
    String observacionesSolicitud,
    String observacionesRevision,
    Instant fechaRevision,
    UUID revisadoPor,
    BadgePlazoDto badgePlazo,
    List<DocumentoNormativaDto> documentos,
    boolean carreraEsNueva,
    boolean carreraTieneHistorial
) {}
