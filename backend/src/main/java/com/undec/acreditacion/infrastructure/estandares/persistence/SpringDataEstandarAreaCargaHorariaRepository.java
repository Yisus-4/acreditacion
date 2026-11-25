package com.undec.acreditacion.infrastructure.estandares.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataEstandarAreaCargaHorariaRepository extends JpaRepository<EstandarAreaCargaHorariaJpaEntity, UUID> {

    List<EstandarAreaCargaHorariaJpaEntity> findByEstandarIdOrderByOrdenAsc(UUID estandarId);

    void deleteByEstandarId(UUID estandarId);
}
