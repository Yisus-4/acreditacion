package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PrioridadActividad;

import java.time.LocalDate;

public record ItemActividadPlanRequest(
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
