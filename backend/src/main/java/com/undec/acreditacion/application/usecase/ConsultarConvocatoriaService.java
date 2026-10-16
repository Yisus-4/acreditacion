package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarConvocatoriaUseCase;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class ConsultarConvocatoriaService implements ConsultarConvocatoriaUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public ConsultarConvocatoriaService(ProcesoAcreditacionRepositoryPort procesoRepository) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
    }

    @Override
    public ProcesoAcreditacion obtenerPorId(UUID id) {
        return procesoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + id));
    }
}
