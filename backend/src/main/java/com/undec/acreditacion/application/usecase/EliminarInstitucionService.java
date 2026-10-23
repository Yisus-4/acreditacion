package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EliminarInstitucionUseCase;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class EliminarInstitucionService implements EliminarInstitucionUseCase {

    private final InstitucionRepositoryPort institucionRepository;

    public EliminarInstitucionService(InstitucionRepositoryPort institucionRepository) {
        this.institucionRepository = Objects.requireNonNull(institucionRepository, "institucionRepository must not be null");
    }

    @Override
    public void eliminar(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("Institución no encontrada: null");
        }
        institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institución no encontrada: " + id));

        institucionRepository.deleteById(id);
    }
}
