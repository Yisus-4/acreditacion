package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataActividadCurricularPlanRepository extends JpaRepository<ActividadCurricularPlanJpaEntity, UUID> {

    List<ActividadCurricularPlanJpaEntity> findByPlanEstudioIdOrderByNumeroOrdenAsc(UUID planEstudioId);

    @Query("SELECT COALESCE(MAX(a.numeroOrden), 0) FROM ActividadCurricularPlanJpaEntity a WHERE a.planEstudio.id = :planEstudioId")
    Integer findMaxNumeroOrdenByPlanEstudioId(@Param("planEstudioId") UUID planEstudioId);

    boolean existsByPlanEstudioIdAndCodigo(UUID planEstudioId, String codigo);

    @Query("SELECT a FROM ActividadCurricularPlanJpaEntity a WHERE a.planEstudio.id = :planEstudioId AND LOWER(a.correlativas) LIKE LOWER(CONCAT('%', :nombre, '%'))")
    List<ActividadCurricularPlanJpaEntity> findCorrelativeDependents(@Param("planEstudioId") UUID planEstudioId, @Param("nombre") String nombre);

    java.util.Optional<ActividadCurricularPlanJpaEntity> findByPlanEstudioIdAndOrigenMaestroId(UUID planEstudioId, UUID origenMaestroId);
}
