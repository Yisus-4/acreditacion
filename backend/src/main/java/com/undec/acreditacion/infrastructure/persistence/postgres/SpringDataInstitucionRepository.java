package com.undec.acreditacion.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataInstitucionRepository extends JpaRepository<InstitucionJpaEntity, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombre(String nombre);

    boolean existsBySigla(String sigla);

    Optional<InstitucionJpaEntity> findByNombreIgnoreCase(String nombre);

    Optional<InstitucionJpaEntity> findByNombre(String nombre);

    Optional<InstitucionJpaEntity> findBySiglaIgnoreCase(String sigla);
}
