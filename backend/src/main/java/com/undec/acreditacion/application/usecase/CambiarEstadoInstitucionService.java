package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CambiarEstadoInstitucionUseCase;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class CambiarEstadoInstitucionService implements CambiarEstadoInstitucionUseCase {

    private final InstitucionRepositoryPort institucionRepository;

    public CambiarEstadoInstitucionService(InstitucionRepositoryPort institucionRepository) {
        this.institucionRepository = Objects.requireNonNull(institucionRepository, "institucionRepository must not be null");
    }

    @Override
    public Institucion cambiarEstado(UUID id, boolean activo) {
        if (id == null) {
            throw new ResourceNotFoundException("Institución no encontrada: null");
        }
        Institucion institucion = institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institución no encontrada: " + id));

        Institucion actualizada = institucion.cambiarEstado(activo);
        return institucionRepository.save(actualizada);
    }
}
