package com.undec.acreditacion.infrastructure.designacion.dto;

import com.undec.acreditacion.domain.entities.EstadoSolicitudDesignacion;
import com.undec.acreditacion.domain.entities.TipoSolicitudDesignacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SolicitudDesignacionDetalleDto(
        UUID id,
        UUID departamentoAcademicoId,
        String departamentoCodigo,
        String departamentoDenominacion,
        UUID periodoAcademicoId,
        String periodoCodigo,
        String periodoDenominacion,
        Integer periodoAnio,
        TipoSolicitudDesignacion tipoSolicitud,
        EstadoSolicitudDesignacion estado,
        boolean editable,
        String observacionesDepartamento,
        String observacionesSga,
        Instant fechaEmision,
        Instant creadoEn,
        Instant actualizadoEn,
        List<DesignacionDocenteItemDto> designaciones
) {}
