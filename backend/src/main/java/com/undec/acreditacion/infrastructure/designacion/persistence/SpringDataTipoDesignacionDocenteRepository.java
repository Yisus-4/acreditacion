package com.undec.acreditacion.infrastructure.designacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTipoDesignacionDocenteRepository extends JpaRepository<TipoDesignacionDocenteJpaEntity, UUID> {

    List<TipoDesignacionDocenteJpaEntity> findAllByOrderByOrdenAscNombreAsc();

    List<TipoDesignacionDocenteJpaEntity> findByActivoTrueOrderByOrdenAscNombreAsc();

    Optional<TipoDesignacionDocenteJpaEntity> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
