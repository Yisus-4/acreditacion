package com.undec.acreditacion.infrastructure.designacion.dto;

import com.undec.acreditacion.domain.entities.EstadoSolicitudDesignacion;
import com.undec.acreditacion.domain.entities.TipoSolicitudDesignacion;

import java.time.Instant;
import java.util.UUID;

public record SolicitudDesignacionResumenDto(
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
        long cantidadDesignaciones,
        long cantidadConAlertaHoras,
        String observacionesDepartamento,
        String observacionesSga,
        Instant fechaEmision,
        Instant creadoEn,
        Instant actualizadoEn
) {}
