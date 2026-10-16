package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPlanTrabajoRepository extends JpaRepository<PlanTrabajoJpaEntity, UUID> {

    Optional<PlanTrabajoJpaEntity> findByProcesoId(UUID procesoId);
}
