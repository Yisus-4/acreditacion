package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.Carrera;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CarreraRepositoryPort {

    Carrera save(Carrera carrera);

    Optional<Carrera> findById(UUID id);

    List<Carrera> findAll();

    List<Carrera> findByUnidadAcademicaId(UUID unidadAcademicaId);

    boolean existsByUnidadAcademicaIdAndNombre(UUID unidadAcademicaId, String nombre);

    void deleteById(UUID id);

    long countByUnidadAcademicaId(UUID unidadAcademicaId);

    boolean existsBySedeId(UUID sedeId);
}
