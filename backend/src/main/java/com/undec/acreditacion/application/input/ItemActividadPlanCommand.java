package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PrioridadActividad;

import java.time.LocalDate;

public record ItemActividadPlanCommand(
        int numeroOrden,
        String codigoActividad,
        String nombre,
        HojaPlanTrabajo hoja,
        LocalDate fechaInicioEstimada,
        LocalDate fechaFinEstimada,
        String responsable,
        PrioridadActividad prioridad
) {
}
