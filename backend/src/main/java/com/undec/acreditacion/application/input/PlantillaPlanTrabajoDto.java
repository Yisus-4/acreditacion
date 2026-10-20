package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.TipoPlanTrabajo;

import java.util.List;

public record PlantillaPlanTrabajoDto(
        TipoPlanTrabajo tipoPlan,
        List<PlantillaActividadDto> actividadesInfo,
        List<PlantillaActividadDto> actividadesConeau
) {
}
