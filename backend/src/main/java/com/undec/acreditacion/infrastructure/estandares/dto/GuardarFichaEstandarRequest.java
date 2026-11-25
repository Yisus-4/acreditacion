package com.undec.acreditacion.infrastructure.estandares.dto;

import java.util.List;

public record GuardarFichaEstandarRequest(
        List<FichaEstandarActividadDto.Respuesta22Dto> respuestas22,
        List<FichaEstandarActividadDto.CargaAreaDto> cargasPorArea,
        FichaEstandarActividadDto.ResumenCargaDto resumenCarga
) {
}
