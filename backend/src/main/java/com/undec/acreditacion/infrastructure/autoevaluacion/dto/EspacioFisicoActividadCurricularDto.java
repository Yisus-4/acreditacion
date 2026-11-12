package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.time.Instant;
import java.util.UUID;

public record EspacioFisicoActividadCurricularDto(
        UUID id,
        UUID espacioFisicoId,
        UUID carreraId,
        String carreraNombre,
        UUID planEstudioId,
        String planEstudioNombre,
        UUID actividadCurricularId,
        String actividadCodigo,
        String actividadNombre,
        Integer comisionesAsignadas,
        String observacionesUso,
        Instant creadoEn
) {}
