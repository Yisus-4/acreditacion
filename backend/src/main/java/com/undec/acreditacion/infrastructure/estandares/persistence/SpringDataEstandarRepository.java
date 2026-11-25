package com.undec.acreditacion.infrastructure.estandares.persistence;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataEstandarRepository extends JpaRepository<EstandarJpaEntity, UUID> {

    Optional<EstandarJpaEntity> findByCarreraIdAndEstadoVigencia(UUID carreraId, EstadoVigenciaEstandar estadoVigencia);

    List<EstandarJpaEntity> findByCarreraIdOrderByActualizadoEnDesc(UUID carreraId);

    List<EstandarJpaEntity> findByEstadoVigenciaOrderByActualizadoEnDesc(EstadoVigenciaEstandar estadoVigencia);

    List<EstandarJpaEntity> findAllByOrderByActualizadoEnDesc();
}
