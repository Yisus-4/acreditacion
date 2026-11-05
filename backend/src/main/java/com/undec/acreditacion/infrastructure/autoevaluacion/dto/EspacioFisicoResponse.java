package com.undec.acreditacion.infrastructure.autoevaluacion.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EspacioFisicoResponse(
        UUID id,
        UUID tipoEspacioId,
        String tipoEspacioCodigo,
        String tipoEspacioDenominacion,
        String denominacion,
        String descripcionInfraestructura,
        Integer cantidad,
        Integer capacidadPersonasSentadas,
        Integer capacidadEstudiantes,
        BigDecimal superficieM2,
        String equipamientoDisponible,
        String estadoConeau,
        Boolean activo,
        UUID espacioPadreId,
        Boolean tieneSubespacios,
        String observacionHigieneSeguridad,
        List<EspacioFisicoResponse> subespacios
) {
}
