package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.Institucion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstitucionRepositoryPort {

    Institucion save(Institucion institucion);

    boolean existsByNombre(String nombre);

    boolean existsBySigla(String sigla);

    Optional<Institucion> findById(UUID id);

    Optional<Institucion> findByNombre(String nombre);

    Optional<Institucion> findBySigla(String sigla);

    List<Institucion> findAll();

    void deleteById(UUID id);
}
