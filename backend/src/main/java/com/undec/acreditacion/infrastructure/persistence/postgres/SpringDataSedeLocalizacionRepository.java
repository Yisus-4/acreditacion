package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataSedeLocalizacionRepository extends JpaRepository<SedeLocalizacionJpaEntity, UUID> {

    List<SedeLocalizacionJpaEntity> findByInstitucionId(UUID institucionId);
}
