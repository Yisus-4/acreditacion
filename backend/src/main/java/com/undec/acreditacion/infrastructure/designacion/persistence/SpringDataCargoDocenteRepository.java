package com.undec.acreditacion.infrastructure.designacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCargoDocenteRepository extends JpaRepository<CargoDocenteJpaEntity, UUID> {

    List<CargoDocenteJpaEntity> findAllByOrderByOrdenAscNombreAsc();

    List<CargoDocenteJpaEntity> findByActivoTrueOrderByOrdenAscNombreAsc();

    Optional<CargoDocenteJpaEntity> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);
}
