package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ListarCarrerasUseCase;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ListarCarrerasService implements ListarCarrerasUseCase {

    private final CarreraRepositoryPort carreraRepository;

    public ListarCarrerasService(CarreraRepositoryPort carreraRepository) {
        this.carreraRepository = Objects.requireNonNull(carreraRepository, "carreraRepository must not be null");
    }

    @Override
    public List<Carrera> listarTodas() {
        return carreraRepository.findAll();
    }

    @Override
    public List<Carrera> listarPorUnidadAcademica(UUID unidadAcademicaId) {
        if (unidadAcademicaId == null) {
            return carreraRepository.findAll();
        }
        return carreraRepository.findByUnidadAcademicaId(unidadAcademicaId);
    }
}
