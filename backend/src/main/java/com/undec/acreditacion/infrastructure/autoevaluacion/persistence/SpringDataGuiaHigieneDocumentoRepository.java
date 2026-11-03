package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataGuiaHigieneDocumentoRepository extends JpaRepository<GuiaHigieneDocumentoJpaEntity, UUID> {

    List<GuiaHigieneDocumentoJpaEntity> findByGuiaIdOrderByCreadoEnAsc(UUID guiaId);
}
