package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDocumentoAcreditacionRepository extends JpaRepository<DocumentoAcreditacionJpaEntity, UUID> {

    List<DocumentoAcreditacionJpaEntity> findByActividadId(UUID actividadId);

    List<DocumentoAcreditacionJpaEntity> findByProcesoId(UUID procesoId);
}
