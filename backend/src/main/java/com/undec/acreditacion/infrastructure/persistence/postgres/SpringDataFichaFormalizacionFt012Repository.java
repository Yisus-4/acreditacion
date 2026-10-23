package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataFichaFormalizacionFt012Repository extends JpaRepository<FichaFormalizacionFt012JpaEntity, UUID> {

    Optional<FichaFormalizacionFt012JpaEntity> findByProcesoId(UUID procesoId);
}
