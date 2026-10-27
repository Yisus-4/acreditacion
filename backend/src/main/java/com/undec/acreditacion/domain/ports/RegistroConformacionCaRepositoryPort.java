package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.RegistroConformacionCa;

import java.util.Optional;
import java.util.UUID;

public interface RegistroConformacionCaRepositoryPort {
    RegistroConformacionCa save(RegistroConformacionCa registro);
    Optional<RegistroConformacionCa> findByProcesoId(UUID procesoId);
    Optional<RegistroConformacionCa> findById(UUID id);
    void deleteById(UUID id);
}
