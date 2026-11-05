package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PrecedenciasPlanTrabajoCatalogo;
import com.undec.acreditacion.domain.entities.PrioridadActividad;

import java.time.LocalDate;
import java.util.List;

public record ItemActividadPlanRequest(
        int numeroOrden,
        String codigoActividad,
        String nombre,
        HojaPlanTrabajo hoja,
        LocalDate fechaInicioEstimada,
        LocalDate fechaFinEstimada,
        String responsable,
        PrioridadActividad prioridad,
        List<String> predecesoras
) {
    public ItemActividadPlanRequest(
            int numeroOrden,
            String codigoActividad,
            String nombre,
            HojaPlanTrabajo hoja,
            LocalDate fechaInicioEstimada,
            LocalDate fechaFinEstimada,
            String responsable,
            PrioridadActividad prioridad
    ) {
        this(
                numeroOrden,
                codigoActividad,
                nombre,
                hoja,
                fechaInicioEstimada,
                fechaFinEstimada,
                responsable,
                prioridad,
                PrecedenciasPlanTrabajoCatalogo.resolver(codigoActividad)
        );
    }
}
