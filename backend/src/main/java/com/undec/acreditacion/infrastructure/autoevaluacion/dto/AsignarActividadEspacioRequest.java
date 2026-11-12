package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.util.UUID;

public record AsignarActividadEspacioRequest(
        UUID carreraId,
        UUID planEstudioId,
        UUID actividadCurricularId,
        Integer comisionesAsignadas,
        String observacionesUso
) {}
