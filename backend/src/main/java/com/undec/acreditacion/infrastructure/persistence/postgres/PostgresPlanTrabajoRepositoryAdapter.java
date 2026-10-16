package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresPlanTrabajoRepositoryAdapter implements PlanTrabajoRepositoryPort {

    private final SpringDataPlanTrabajoRepository planRepository;
    private final SpringDataActividadPlanTrabajoRepository actividadRepository;
    private final PlanTrabajoPersistenceMapper mapper;

    public PostgresPlanTrabajoRepositoryAdapter(
            SpringDataPlanTrabajoRepository planRepository,
            SpringDataActividadPlanTrabajoRepository actividadRepository,
            PlanTrabajoPersistenceMapper mapper) {
        this.planRepository = Objects.requireNonNull(planRepository);
        this.actividadRepository = Objects.requireNonNull(actividadRepository);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public PlanTrabajo save(PlanTrabajo plan) {
        PlanTrabajoJpaEntity planEntity = mapper.toJpa(plan);
        PlanTrabajoJpaEntity savedPlan = planRepository.save(planEntity);

        // Guardar las actividades asociadas al plan
        if (plan.getActividades() != null) {
            for (ActividadPlanTrabajo act : plan.getActividades()) {
                actividadRepository.save(mapper.toJpaActividad(act));
            }
        }

        List<ActividadPlanTrabajoJpaEntity> actividadesEntities =
                actividadRepository.findByPlanTrabajoIdOrderByNumeroOrdenAsc(savedPlan.getId());
        return mapper.toDomain(savedPlan, actividadesEntities);
    }

    @Override
    public Optional<PlanTrabajo> findById(UUID id) {
        return planRepository.findById(id).map(planEntity -> {
            List<ActividadPlanTrabajoJpaEntity> actividades =
                    actividadRepository.findByPlanTrabajoIdOrderByNumeroOrdenAsc(planEntity.getId());
            return mapper.toDomain(planEntity, actividades);
        });
    }

    @Override
    public Optional<PlanTrabajo> findByProcesoId(UUID procesoId) {
        return planRepository.findByProcesoId(procesoId).map(planEntity -> {
            List<ActividadPlanTrabajoJpaEntity> actividades =
                    actividadRepository.findByPlanTrabajoIdOrderByNumeroOrdenAsc(planEntity.getId());
            return mapper.toDomain(planEntity, actividades);
        });
    }

    @Override
    public Optional<ActividadPlanTrabajo> findActividadById(UUID actividadId) {
        return actividadRepository.findById(actividadId).map(mapper::toDomainActividad);
    }

    @Override
    public ActividadPlanTrabajo saveActividad(ActividadPlanTrabajo actividad) {
        ActividadPlanTrabajoJpaEntity entity = mapper.toJpaActividad(actividad);
        ActividadPlanTrabajoJpaEntity saved = actividadRepository.save(entity);
        return mapper.toDomainActividad(saved);
    }
}
