package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataRegistroConvocatoriaRepository extends JpaRepository<RegistroConvocatoriaJpaEntity, UUID> {

    Optional<RegistroConvocatoriaJpaEntity> findByProcesoId(UUID procesoId);
}
