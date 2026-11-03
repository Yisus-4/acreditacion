package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataInmuebleDocumentacionRespaldoRepository extends JpaRepository<InmuebleDocumentacionRespaldoJpaEntity, UUID> {

    List<InmuebleDocumentacionRespaldoJpaEntity> findByInmuebleIdOrderByFechaSubidaDesc(UUID inmuebleId);
}
