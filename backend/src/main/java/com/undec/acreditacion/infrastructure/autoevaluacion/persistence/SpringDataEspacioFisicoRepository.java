package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataEspacioFisicoRepository extends JpaRepository<EspacioFisicoJpaEntity, UUID> {

    List<EspacioFisicoJpaEntity> findByInmuebleIdAndActivoTrue(UUID inmuebleId);
}
