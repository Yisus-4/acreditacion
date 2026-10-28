package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.RegistroUsuariosAtenea;

import java.util.Optional;
import java.util.UUID;

public interface RegistroUsuariosAteneaRepositoryPort {
    RegistroUsuariosAtenea save(RegistroUsuariosAtenea registro);
    Optional<RegistroUsuariosAtenea> findByProcesoId(UUID procesoId);
    Optional<RegistroUsuariosAtenea> findById(UUID id);
    void deleteById(UUID id);
}
