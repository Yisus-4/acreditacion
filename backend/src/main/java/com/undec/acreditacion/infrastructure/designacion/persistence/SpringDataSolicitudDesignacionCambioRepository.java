package com.undec.acreditacion.infrastructure.designacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataSolicitudDesignacionCambioRepository extends JpaRepository<SolicitudDesignacionCambioJpaEntity, UUID> {

    List<SolicitudDesignacionCambioJpaEntity> findBySolicitudIdOrderByFechaCambioDesc(UUID solicitudId);
}
