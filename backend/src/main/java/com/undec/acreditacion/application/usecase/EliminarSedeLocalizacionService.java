package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EliminarSedeLocalizacionUseCase;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public final class EliminarSedeLocalizacionService implements EliminarSedeLocalizacionUseCase {

    private final SedeLocalizacionRepositoryPort sedeLocalizacionRepository;
    private final CarreraRepositoryPort carreraRepository;

    public EliminarSedeLocalizacionService(SedeLocalizacionRepositoryPort sedeLocalizacionRepository,
                                          CarreraRepositoryPort carreraRepository) {
        this.sedeLocalizacionRepository = Objects.requireNonNull(sedeLocalizacionRepository, "El repositorio de sedes es obligatorio");
        this.carreraRepository = Objects.requireNonNull(carreraRepository, "El repositorio de carreras es obligatorio");
    }

    @Override
    public void eliminar(UUID id) {
        Objects.requireNonNull(id, "El ID de la sede es obligatorio");

        sedeLocalizacionRepository.findById(id)
                .orElseThrow(() -> new DomainValidationException("No se encontró la sede con ID: " + id));

        if (carreraRepository.existsBySedeId(id)) {
            throw new DomainValidationException("No se puede eliminar la sede porque está asignada a una o más carreras");
        }

        sedeLocalizacionRepository.deleteById(id);
    }
}
