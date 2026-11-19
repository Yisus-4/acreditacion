package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
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

    List<PlanEstudioFt001JpaEntity> findByCarreraIdAndEstadoVigenciaAndEsMaestroTrue(UUID carreraId, EstadoVigenciaPlan estadoVigencia);

    List<PlanEstudioFt001JpaEntity> findByCarreraIdAndEstadoVigenciaAndEsMaestroTrueOrderByAnioAprobacionDesc(UUID carreraId, EstadoVigenciaPlan estadoVigencia);

    List<PlanEstudioFt001JpaEntity> findByCarreraIdAndEsMaestroTrueOrderByAnioAprobacionDesc(UUID carreraId);

    List<PlanEstudioFt001JpaEntity> findByEsMaestroTrueOrderByAnioAprobacionDesc();

    List<PlanEstudioFt001JpaEntity> findByEsMaestroTrueAndEstadoVigenciaOrderByAnioAprobacionDesc(EstadoVigenciaPlan estadoVigencia);

    long countByEsMaestroTrueAndEstadoVigencia(EstadoVigenciaPlan estadoVigencia);

    Optional<PlanEstudioFt001JpaEntity> findByProcesoIdAndOrigenMaestroId(UUID procesoId, UUID origenMaestroId);

    List<PlanEstudioFt001JpaEntity> findByOrigenMaestroIdAndActivoTrue(UUID origenMaestroId);
}
