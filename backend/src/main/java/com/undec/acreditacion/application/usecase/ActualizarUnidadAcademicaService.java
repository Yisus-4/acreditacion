package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ActualizarUnidadAcademicaCommand;
import com.undec.acreditacion.application.input.ActualizarUnidadAcademicaUseCase;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;

import java.util.Objects;

public final class ActualizarUnidadAcademicaService implements ActualizarUnidadAcademicaUseCase {

    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;

    public ActualizarUnidadAcademicaService(UnidadAcademicaRepositoryPort unidadAcademicaRepository) {
        this.unidadAcademicaRepository = Objects.requireNonNull(unidadAcademicaRepository, "El repositorio de unidad académica es obligatorio");
    }

    @Override
    public UnidadAcademica actualizar(ActualizarUnidadAcademicaCommand command) {
        Objects.requireNonNull(command, "El comando de actualización es obligatorio");
        Objects.requireNonNull(command.id(), "El ID de la unidad académica es obligatorio");

        UnidadAcademica ua = unidadAcademicaRepository.findById(command.id())
                .orElseThrow(() -> new DomainValidationException("No se encontró la unidad académica con ID: " + command.id()));

        if (!ua.getNombre().equalsIgnoreCase(command.nombre().trim())) {
            if (unidadAcademicaRepository.existsByInstitucionIdAndNombre(ua.getInstitucionId(), command.nombre().trim())) {
                throw new DomainValidationException("Ya existe una unidad académica con el nombre: " + command.nombre());
            }
        }

        ua.actualizar(command.nombre(), command.sigla(), command.descripcion());
        return unidadAcademicaRepository.save(ua);
    }
}
