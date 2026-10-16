package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EliminarUnidadAcademicaUseCase;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public final class EliminarUnidadAcademicaService implements EliminarUnidadAcademicaUseCase {

    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private final CarreraRepositoryPort carreraRepository;

    public EliminarUnidadAcademicaService(UnidadAcademicaRepositoryPort unidadAcademicaRepository,
                                           CarreraRepositoryPort carreraRepository) {
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository, "El repositorio de unidad académica es obligatorio");
        this.carreraRepository = Objects.requireNonNull(carreraRepository, "El repositorio de carrera es obligatorio");
    }

    @Override
    public void eliminar(UUID id) {
        Objects.requireNonNull(id, "El ID de la unidad académica es obligatorio");

        unidadAcademicaRepository.findById(id)
                .orElseThrow(() -> new DomainValidationException("No se encontró la unidad académica con ID: " + id));

        long count = carreraRepository.countByUnidadAcademicaId(id);
        if (count > 0) {
            throw new DomainValidationException("No se puede eliminar la unidad académica porque tiene carreras asociadas");
        }

        unidadAcademicaRepository.deleteById(id);
    }
}
