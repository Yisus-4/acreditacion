package com.undec.acreditacion.domain.ports;

import com.undec.acreditacion.domain.entities.SedeLocalizacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SedeLocalizacionRepositoryPort {

    SedeLocalizacion save(SedeLocalizacion sede);

    Optional<SedeLocalizacion> findById(UUID id);

    List<SedeLocalizacion> findAll();

    List<SedeLocalizacion> findByInstitucionId(UUID institucionId);

    List<SedeLocalizacion> findByIds(List<UUID> ids);

    void deleteById(UUID id);
}
