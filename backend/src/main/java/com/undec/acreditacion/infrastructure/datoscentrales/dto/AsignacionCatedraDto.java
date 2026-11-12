package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import java.util.UUID;

public record AsignacionCatedraDto(
        UUID id,
        UUID carreraId,
        String carreraNombre,
        UUID planEstudioId,
        String planEstudioNombre,
        UUID actividadCurricularId,
        String actividadCurricularNombre,
        String rolEnCatedra,
        Integer cargaHorariaSemanal
) {
}
