package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAutoevaluacionCarreraAmbitoRepository extends JpaRepository<AutoevaluacionCarreraAmbitoJpaEntity, UUID> {

    List<AutoevaluacionCarreraAmbitoJpaEntity> findByProcesoIdOrderByAmbitoNumeroOrdenAsc(UUID procesoId);

    Optional<AutoevaluacionCarreraAmbitoJpaEntity> findByProcesoIdAndAmbitoId(UUID procesoId, UUID ambitoId);

    List<AutoevaluacionCarreraAmbitoJpaEntity> findByProcesoIdAndCarreraIdOrderByAmbitoNumeroOrdenAsc(UUID procesoId, UUID carreraId);

    Optional<AutoevaluacionCarreraAmbitoJpaEntity> findByProcesoIdAndCarreraIdAndAmbitoId(UUID procesoId, UUID carreraId, UUID ambitoId);

    List<AutoevaluacionCarreraAmbitoJpaEntity> findByCarreraIdOrderByAmbitoNumeroOrdenAsc(UUID carreraId);

    @Query("SELECT a FROM AutoevaluacionCarreraAmbitoJpaEntity a WHERE a.procesoId = :procesoId AND a.utilizadoPorCarrera = true AND LOWER(a.asignaturasVinculadas) LIKE LOWER(CONCAT('%', :subjectName, '%'))")
    List<AutoevaluacionCarreraAmbitoJpaEntity> findActiveUsagesBySubject(@Param("procesoId") UUID procesoId, @Param("subjectName") String subjectName);

    List<AutoevaluacionCarreraAmbitoJpaEntity> findByProcesoIdAndUtilizadoPorCarreraTrue(UUID procesoId);

    List<AutoevaluacionCarreraAmbitoJpaEntity> findByAmbitoId(UUID ambitoId);
}
