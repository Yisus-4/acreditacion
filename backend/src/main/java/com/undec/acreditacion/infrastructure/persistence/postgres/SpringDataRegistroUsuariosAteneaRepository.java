package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataRegistroUsuariosAteneaRepository extends JpaRepository<RegistroUsuariosAteneaJpaEntity, UUID> {
    Optional<RegistroUsuariosAteneaJpaEntity> findByProcesoId(UUID procesoId);
}
