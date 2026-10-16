package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ListarUnidadesAcademicasUseCase;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ListarUnidadesAcademicasService implements ListarUnidadesAcademicasUseCase {

    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;

    public ListarUnidadesAcademicasService(UnidadAcademicaRepositoryPort unidadAcademicaRepository) {
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository, "unidadAcademicaRepository must not be null");
    }

    @Override
    public List<UnidadAcademica> listarTodas() {
        return unidadAcademicaRepository.findAll();
    }

    @Override
    public List<UnidadAcademica> listarPorInstitucion(UUID institucionId) {
        if (institucionId == null) {
            return unidadAcademicaRepository.findAll();
        }
        return unidadAcademicaRepository.findByInstitucionId(institucionId);
    }
}
