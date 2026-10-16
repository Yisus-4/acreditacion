package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CambiarEstadoSedeLocalizacionUseCase;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public final class CambiarEstadoSedeLocalizacionService implements CambiarEstadoSedeLocalizacionUseCase {

    private final SedeLocalizacionRepositoryPort sedeLocalizacionRepository;

    public CambiarEstadoSedeLocalizacionService(SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        this.sedeLocalizacionRepository = Objects.requireNonNull(sedeLocalizacionRepository, "El repositorio de sedes es obligatorio");
    }

    @Override
    public SedeLocalizacion cambiarEstado(UUID id, boolean activo) {
        Objects.requireNonNull(id, "El ID de la sede es obligatorio");

        SedeLocalizacion sede = sedeLocalizacionRepository.findById(id)
                .orElseThrow(() -> new DomainValidationException("No se encontró la sede con ID: " + id));

        sede.cambiarEstado(activo);
        return sedeLocalizacionRepository.save(sede);
    }
}
