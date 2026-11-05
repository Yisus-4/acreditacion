package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPlanEstudioFt001Repository extends JpaRepository<PlanEstudioFt001JpaEntity, UUID> {

    List<PlanEstudioFt001JpaEntity> findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(UUID carreraId);

    Optional<PlanEstudioFt001JpaEntity> findFirstByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(UUID carreraId);

    List<PlanEstudioFt001JpaEntity> findByProcesoIdAndActivoTrue(UUID procesoId);
}
