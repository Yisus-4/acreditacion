package com.undec.acreditacion.infrastructure.estandares.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataActividadCurricularCargaHorariaResumenRepository
        extends JpaRepository<ActividadCurricularCargaHorariaResumenJpaEntity, UUID> {

    List<ActividadCurricularCargaHorariaResumenJpaEntity> findByActividadCurricularIdIn(Collection<UUID> actividadCurricularIds);
}
