package com.undec.acreditacion.infrastructure.designacion.dto;

import java.util.List;

public record CatalogosDocentesDto(
        List<DepartamentoAcademicoDto> departamentos,
        List<CargoDocenteDto> cargos,
        List<DedicacionDocenteDto> dedicaciones,
        List<TipoDesignacionDocenteDto> tiposDesignacion,
        List<PeriodoAcademicoDto> periodos
) {
}
