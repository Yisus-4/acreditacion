package com.undec.acreditacion.infrastructure.estandares.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataEstandarItem22Repository extends JpaRepository<EstandarItem22JpaEntity, UUID> {

    List<EstandarItem22JpaEntity> findByGrupoIdInOrderByOrdenAsc(Collection<UUID> grupoIds);

    @Query("SELECT i FROM EstandarItem22JpaEntity i WHERE i.grupo.estandar.id = :estandarId ORDER BY i.grupo.orden ASC, i.orden ASC")
    List<EstandarItem22JpaEntity> findByGrupoEstandarIdOrderByOrdenAsc(@Param("estandarId") UUID estandarId);
}
