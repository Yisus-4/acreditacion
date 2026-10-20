package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PrioridadActividad;

public record PlantillaActividadDto(
        int ordenSugerido,
        String codigoActividad,
        String nombre,
        HojaPlanTrabajo hoja,
        PrioridadActividad prioridadSugerida,
        String responsableSugerido
) {
}
