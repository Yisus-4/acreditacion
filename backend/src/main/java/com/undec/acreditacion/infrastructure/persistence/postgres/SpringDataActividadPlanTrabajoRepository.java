package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataActividadPlanTrabajoRepository extends JpaRepository<ActividadPlanTrabajoJpaEntity, UUID> {

    List<ActividadPlanTrabajoJpaEntity> findByPlanTrabajoIdOrderByNumeroOrdenAsc(UUID planTrabajoId);
}
