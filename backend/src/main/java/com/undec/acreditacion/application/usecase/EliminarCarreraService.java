package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EliminarCarreraUseCase;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public final class EliminarCarreraService implements EliminarCarreraUseCase {

    private final CarreraRepositoryPort carreraRepository;

    public EliminarCarreraService(CarreraRepositoryPort carreraRepository) {
        this.carreraRepository = Objects.requireNonNull(carreraRepository, "El repositorio de carreras es obligatorio");
    }

    @Override
    public void eliminar(UUID id) {
        Objects.requireNonNull(id, "El ID de la carrera es obligatorio");

        carreraRepository.findById(id)
                .orElseThrow(() -> new DomainValidationException("No se encontró la carrera con ID: " + id));

        carreraRepository.deleteById(id);
    }
}
