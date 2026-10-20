package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EliminarConvocatoriaUseCase;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class EliminarConvocatoriaService implements EliminarConvocatoriaUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public EliminarConvocatoriaService(ProcesoAcreditacionRepositoryPort procesoRepository) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository, "El repositorio de procesos de acreditación es obligatorio");
    }

    @Override
    public void ejecutar(UUID procesoId) {
        Objects.requireNonNull(procesoId, "El ID del proceso de acreditación es obligatorio");

        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        procesoRepository.deleteById(procesoId);
    }
}
