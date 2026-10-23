package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.Convocatoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConvocatoriaEscuelaRepositoryPort {
    Convocatoria save(Convocatoria convocatoria);
    Optional<Convocatoria> findById(UUID id);
    List<Convocatoria> findAll();
    long countByAnio(int anio);
    void deleteById(UUID id);
}
