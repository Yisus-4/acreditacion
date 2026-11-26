package com.undec.acreditacion.infrastructure.designacion.dto;

import com.undec.acreditacion.domain.entities.EstadoItemDesignacion;

import java.util.List;
import java.util.UUID;

public record DesignacionDocenteItemDto(
        UUID id,
        UUID solicitudId,
        UUID designacionOrigenId,
        UUID docenteId,
        String docenteDni,
        String docenteLegajo,
        String docenteApellido,
        String docenteNombre,
        UUID cargoDocenteId,
        String cargoCodigo,
        String cargoNombre,
        UUID dedicacionDocenteId,
        String dedicacionCodigo,
        String dedicacionNombre,
        Integer horasDedicacion,
        UUID tipoDesignacionId,
        String tipoDesignacionCodigo,
        String tipoDesignacionNombre,
        String nroDesignacionMapuche,
        EstadoItemDesignacion estadoItem,
        Integer horasClases,
        Integer horasInvestigacion,
        Integer horasExtension,
        Integer horasGestion,
        Integer horasDireccionTesis,
        Integer horasOtras,
        int totalHorasDistribuidas,
        int horasSinDistribuir,
        boolean tieneAlertaHorasSinDistribuir,
        int totalHorasDocentePeriodo,
        String observaciones,
        List<AsignacionCurricularPeriodoDto> asignaciones
) {}
