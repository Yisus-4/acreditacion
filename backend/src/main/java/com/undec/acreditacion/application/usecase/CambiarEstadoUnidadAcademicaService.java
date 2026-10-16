package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CambiarEstadoUnidadAcademicaUseCase;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public final class CambiarEstadoUnidadAcademicaService implements CambiarEstadoUnidadAcademicaUseCase {

    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;

    public CambiarEstadoUnidadAcademicaService(UnidadAcademicaRepositoryPort unidadAcademicaRepository) {
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository, "El repositorio de unidad académica es obligatorio");
    }

    @Override
    public UnidadAcademica cambiarEstado(UUID id, boolean activo) {
        Objects.requireNonNull(id, "El ID de la unidad académica es obligatorio");

        UnidadAcademica ua = unidadAcademicaRepository.findById(id)
                .orElseThrow(() -> new DomainValidationException("No se encontró la unidad académica con ID: " + id));

        ua.cambiarEstado(activo);
        return unidadAcademicaRepository.save(ua);
    }
}
