package com.undec.acreditacion.application.input;

import com.undec.acreditacion.domain.entities.TipoPlanTrabajo;

import java.util.UUID;

public interface ObtenerPlantillaPlanTrabajoUseCase {
    PlantillaPlanTrabajoDto obtenerPorProcesoId(UUID procesoId);
    PlantillaPlanTrabajoDto obtenerPorTipo(TipoPlanTrabajo tipoPlan);
}
