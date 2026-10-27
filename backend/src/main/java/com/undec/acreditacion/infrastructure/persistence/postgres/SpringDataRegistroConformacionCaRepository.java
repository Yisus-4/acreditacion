package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRegistroConformacionCaRepository extends JpaRepository<RegistroConformacionCaJpaEntity, UUID> {
    Optional<RegistroConformacionCaJpaEntity> findByProcesoId(UUID procesoId);
}
