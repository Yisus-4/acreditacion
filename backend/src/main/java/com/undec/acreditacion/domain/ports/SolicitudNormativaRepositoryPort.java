package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.SolicitudNormativaCarrera;

import java.util.Optional;
import java.util.UUID;

public interface SolicitudNormativaRepositoryPort {
    SolicitudNormativaCarrera save(SolicitudNormativaCarrera solicitud);
    Optional<SolicitudNormativaCarrera> findById(UUID id);
    Optional<SolicitudNormativaCarrera> findByProcesoId(UUID procesoId);
    Optional<SolicitudNormativaCarrera> findByActividadId(UUID actividadId);
}
