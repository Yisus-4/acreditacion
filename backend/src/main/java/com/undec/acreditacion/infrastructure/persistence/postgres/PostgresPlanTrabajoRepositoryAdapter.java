package com.undec.acreditacion.infrastructure.persistence.postgres;

import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

        // Limpieza de actividades huérfanas si fueron reemplazadas durante la elaboración
        List<ActividadPlanTrabajoJpaEntity> existing =
                actividadRepository.findByPlanTrabajoIdOrderByNumeroOrdenAsc(savedPlan.getId());
        Set<UUID> currentIds = plan.getActividades() != null
                ? plan.getActividades().stream()
                        .map(ActividadPlanTrabajo::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
                : Set.of();

        List<ActividadPlanTrabajoJpaEntity> toDelete = existing.stream()
                .filter(oldAct -> !currentIds.contains(oldAct.getId()))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            actividadRepository.deleteAll(toDelete);
            actividadRepository.flush();
        }

        // Para las actividades que ya existen en la base de datos y se van a actualizar/conservar,
        // actualizar su numeroOrden temporalmente a números negativos únicos (ej: -1000 - idx) y ejecutar flush().
        // Esto evita la colisión de clave duplicada en PostgreSQL uq_actividad_plan_orden UNIQUE (plan_trabajo_id, numero_orden).
        List<ActividadPlanTrabajoJpaEntity> toKeep = existing.stream()
                .filter(oldAct -> currentIds.contains(oldAct.getId()))
                .collect(Collectors.toList());
        if (!toKeep.isEmpty()) {
            for (int i = 0; i < toKeep.size(); i++) {
                toKeep.get(i).setNumeroOrden(-1000 - i);
            }
            actividadRepository.saveAll(toKeep);
            actividadRepository.flush();
        }

        // Guardar las actividades asociadas al plan
        if (plan.getActividades() != null) {
            for (ActividadPlanTrabajo act : plan.getActividades()) {
                actividadRepository.save(mapper.toJpaActividad(act));
            }
            actividadRepository.flush();

            // Restaurar numeroOrden en los objetos en memoria para consistencia
            Map<UUID, Integer> targetOrders = plan.getActividades().stream()
                    .filter(a -> a.getId() != null)
                    .collect(Collectors.toMap(ActividadPlanTrabajo::getId, ActividadPlanTrabajo::getNumeroOrden, (o1, o2) -> o1));
            for (ActividadPlanTrabajoJpaEntity oldAct : toKeep) {
                Integer finalOrder = targetOrders.get(oldAct.getId());
                if (finalOrder != null) {
                    oldAct.setNumeroOrden(finalOrder);
                }
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
