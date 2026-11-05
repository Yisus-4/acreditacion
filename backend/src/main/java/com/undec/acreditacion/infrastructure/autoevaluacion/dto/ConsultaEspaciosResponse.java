package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ConsultaEspaciosResponse(
        int totalAulas,
        int capacidadTotalPersonas,
        BigDecimal superficieTotalM2,
        List<EspacioFisicoConsultaItem> espacios
) {
    public record EspacioFisicoConsultaItem(
            UUID id,
            UUID inmuebleId,
            String inmuebleNombre,
            UUID sedeId,
            String sedeNombre,
            UUID tipoEspacioId,
            String tipoEspacioDenominacion,
            String denominacion,
            String descripcionInfraestructura,
            Integer cantidad,
            Integer capacidadPersonasSentadas,
            BigDecimal superficieM2,
            String observacionHigieneSeguridad,
            Boolean esSubespacio,
            UUID espacioPadreId,
            String espacioPadreDenominacion
    ) {}
}
