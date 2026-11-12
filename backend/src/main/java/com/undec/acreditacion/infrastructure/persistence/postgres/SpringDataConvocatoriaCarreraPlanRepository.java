package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataConvocatoriaCarreraPlanRepository extends JpaRepository<ConvocatoriaCarreraPlanJpaEntity, UUID> {

    List<ConvocatoriaCarreraPlanJpaEntity> findByConvocatoriaCarreraId(UUID convocatoriaCarreraId);

    List<ConvocatoriaCarreraPlanJpaEntity> findByConvocatoriaCarreraIdAndSeleccionadoPorDeTrue(UUID convocatoriaCarreraId);

    List<ConvocatoriaCarreraPlanJpaEntity> findByCarreraId(UUID carreraId);

    Optional<ConvocatoriaCarreraPlanJpaEntity> findByConvocatoriaCarreraIdAndPlanEstudioMaestroId(UUID convocatoriaCarreraId, UUID planEstudioMaestroId);
}
