package com.undec.acreditacion.infrastructure.estandares.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataActividadCurricularCargaHorariaAreaRepository
        extends JpaRepository<ActividadCurricularCargaHorariaAreaJpaEntity, UUID> {

    List<ActividadCurricularCargaHorariaAreaJpaEntity> findByActividadCurricularId(UUID actividadCurricularId);

    List<ActividadCurricularCargaHorariaAreaJpaEntity> findByActividadCurricularIdIn(Collection<UUID> actividadCurricularIds);

    void deleteByActividadCurricularId(UUID actividadCurricularId);
}
