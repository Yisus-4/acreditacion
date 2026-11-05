package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PrecedenciasPlanTrabajoCatalogo;
import com.undec.acreditacion.domain.entities.PrioridadActividad;

import java.util.List;

public record PlantillaActividadDto(
        int ordenSugerido,
        String codigoActividad,
        String nombre,
        HojaPlanTrabajo hoja,
        PrioridadActividad prioridadSugerida,
        String responsableSugerido,
        List<String> predecesoras
) {
    public PlantillaActividadDto(
            int ordenSugerido,
            String codigoActividad,
            String nombre,
            HojaPlanTrabajo hoja,
            PrioridadActividad prioridadSugerida,
            String responsableSugerido
    ) {
        this(
                ordenSugerido,
                codigoActividad,
                nombre,
                hoja,
                prioridadSugerida,
                responsableSugerido,
                PrecedenciasPlanTrabajoCatalogo.resolver(codigoActividad)
        );
    }
}
