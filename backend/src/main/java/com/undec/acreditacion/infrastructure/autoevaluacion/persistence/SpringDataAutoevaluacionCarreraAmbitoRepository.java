package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAutoevaluacionCarreraAmbitoRepository extends JpaRepository<AutoevaluacionCarreraAmbitoJpaEntity, UUID> {

    List<AutoevaluacionCarreraAmbitoJpaEntity> findByProcesoIdOrderByAmbitoNumeroOrdenAsc(UUID procesoId);

    Optional<AutoevaluacionCarreraAmbitoJpaEntity> findByProcesoIdAndAmbitoId(UUID procesoId, UUID ambitoId);
}
