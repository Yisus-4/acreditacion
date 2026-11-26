package com.undec.acreditacion.infrastructure.designacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataDedicacionDocenteRepository extends JpaRepository<DedicacionDocenteJpaEntity, UUID> {

    List<DedicacionDocenteJpaEntity> findAllByOrderByOrdenAscHorasSemanalesDesc();

    List<DedicacionDocenteJpaEntity> findByActivoTrueOrderByOrdenAscHorasSemanalesDesc();

    Optional<DedicacionDocenteJpaEntity> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
