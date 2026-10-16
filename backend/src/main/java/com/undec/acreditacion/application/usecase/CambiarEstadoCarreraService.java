package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CambiarEstadoCarreraUseCase;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public final class CambiarEstadoCarreraService implements CambiarEstadoCarreraUseCase {

    private final CarreraRepositoryPort carreraRepository;

    public CambiarEstadoCarreraService(CarreraRepositoryPort carreraRepository) {
        this.carreraRepository = Objects.requireNonNull(carreraRepository, "El repositorio de carreras es obligatorio");
    }

    @Override
    public Carrera cambiarEstado(UUID id, boolean activo) {
        Objects.requireNonNull(id, "El ID de la carrera es obligatorio");

        Carrera carrera = carreraRepository.findById(id)
                .orElseThrow(() -> new DomainValidationException("No se encontró la carrera con ID: " + id));

        carrera.cambiarEstado(activo);
        return carreraRepository.save(carrera);
    }
}
