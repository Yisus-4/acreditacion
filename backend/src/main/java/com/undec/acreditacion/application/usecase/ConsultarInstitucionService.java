package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarInstitucionUseCase;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class ConsultarInstitucionService implements ConsultarInstitucionUseCase {

    private final InstitucionRepositoryPort institucionRepository;

    public ConsultarInstitucionService(InstitucionRepositoryPort institucionRepository) {
        this.institucionRepository = Objects.requireNonNull(institucionRepository, "institucionRepository must not be null");
    }

    @Override
    public Institucion consultarPorId(UUID id) {
        if (id == null) {
            throw new ResourceNotFoundException("Institución no encontrada: null");
        }
        return institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institución no encontrada: " + id));
    }
}
