package com.undec.acreditacion.infrastructure.estandares.dto;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.TipoEvaluacionEstandar22;
import com.undec.acreditacion.domain.entities.TipoSeccionEstandar22;

import java.time.Instant;
import java.util.UUID;

public record EstandarResumenDto(
        UUID id,
        UUID carreraId,
        String carreraNombre,
        String carreraSigla,
        String nombre,
        String resolucionMinisterial,
        Integer anioVigencia,
        EstadoVigenciaEstandar estadoVigencia,
        TipoSeccionEstandar22 tipoSeccion22,
        TipoEvaluacionEstandar22 tipoEvaluacion22,
        int cantidadGrupos22,
        int cantidadItems22,
        int cantidadAreas3,
        long planesAsociados,
        boolean editable,
        Instant creadoEn,
        Instant actualizadoEn
) {
}
