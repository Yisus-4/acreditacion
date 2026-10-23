package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataConvocatoriaCarreraRepository extends JpaRepository<ConvocatoriaCarreraJpaEntity, UUID> {
    List<ConvocatoriaCarreraJpaEntity> findByConvocatoriaId(UUID convocatoriaId);
    List<ConvocatoriaCarreraJpaEntity> findByCarreraId(UUID carreraId);
}
