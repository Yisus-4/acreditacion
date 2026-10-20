package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataDocumentoNormativaRepository extends JpaRepository<DocumentoNormativaJpaEntity, UUID> {
    List<DocumentoNormativaJpaEntity> findBySolicitudNormativaIdOrderByCreadoEnAsc(UUID solicitudNormativaId);
    List<DocumentoNormativaJpaEntity> findByProcesoIdOrderByCreadoEnAsc(UUID procesoId);
}
