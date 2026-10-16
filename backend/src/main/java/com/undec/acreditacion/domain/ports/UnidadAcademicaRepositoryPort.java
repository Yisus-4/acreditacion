package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.UnidadAcademica;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnidadAcademicaRepositoryPort {

    UnidadAcademica save(UnidadAcademica unidadAcademica);

    Optional<UnidadAcademica> findById(UUID id);

    List<UnidadAcademica> findAll();

    List<UnidadAcademica> findByInstitucionId(UUID institucionId);

    boolean existsByInstitucionIdAndNombre(UUID institucionId, String nombre);

    void deleteById(UUID id);
}
