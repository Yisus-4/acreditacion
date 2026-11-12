package com.undec.acreditacion.infrastructure.datoscentrales.dto;

import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;

public record CambiarVigenciaPlanRequest(
        EstadoVigenciaPlan estadoVigencia
) {
}
