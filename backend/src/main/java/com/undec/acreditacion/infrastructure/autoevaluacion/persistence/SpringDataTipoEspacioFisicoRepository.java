package com.undec.acreditacion.infrastructure.autoevaluacion.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTipoEspacioFisicoRepository extends JpaRepository<TipoEspacioFisicoJpaEntity, UUID> {

    List<TipoEspacioFisicoJpaEntity> findByActivoTrueOrderByDenominacionAsc();

    Optional<TipoEspacioFisicoJpaEntity> findByCodigoIgnoreCase(String codigo);
}
