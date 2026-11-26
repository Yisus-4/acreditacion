package com.undec.acreditacion.infrastructure.designacion.dto;

import java.util.List;
import java.util.UUID;

public record VistaConsolidadaPeriodoDto(
        UUID departamentoAcademicoId,
        String departamentoDenominacion,
        UUID periodoAcademicoId,
        String periodoDenominacion,
        Integer periodoAnio,
        int totalSolicitudesIncluidas,
        int totalDesignacionesVigentes,
        List<DesignacionDocenteItemDto> designacionesVigentes
) {}
