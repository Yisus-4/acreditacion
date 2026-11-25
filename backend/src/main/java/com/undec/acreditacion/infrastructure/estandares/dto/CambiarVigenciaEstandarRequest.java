package com.undec.acreditacion.infrastructure.estandares.dto;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;

public record CambiarVigenciaEstandarRequest(EstadoVigenciaEstandar estadoVigencia) {
}
