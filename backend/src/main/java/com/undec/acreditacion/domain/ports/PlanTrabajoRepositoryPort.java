package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;

import java.util.Optional;
import java.util.UUID;

public interface PlanTrabajoRepositoryPort {

    PlanTrabajo save(PlanTrabajo plan);

    Optional<PlanTrabajo> findById(UUID id); // H-01: Resuelve relación hacia procesoId

    Optional<PlanTrabajo> findByProcesoId(UUID procesoId);

    Optional<ActividadPlanTrabajo> findActividadById(UUID actividadId);

    ActividadPlanTrabajo saveActividad(ActividadPlanTrabajo actividad);
}
