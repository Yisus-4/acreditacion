package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.RegistroConvocatoria;

import java.util.Optional;
import java.util.UUID;

public interface RegistroConvocatoriaRepositoryPort {

    RegistroConvocatoria save(RegistroConvocatoria registro);

    Optional<RegistroConvocatoria> findByProcesoId(UUID procesoId);
}
