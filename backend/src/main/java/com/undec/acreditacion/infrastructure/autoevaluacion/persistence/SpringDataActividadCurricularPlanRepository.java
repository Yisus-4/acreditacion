package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataActividadCurricularPlanRepository extends JpaRepository<ActividadCurricularPlanJpaEntity, UUID> {

    List<ActividadCurricularPlanJpaEntity> findByPlanEstudioIdOrderByNumeroOrdenAsc(UUID planEstudioId);
}
