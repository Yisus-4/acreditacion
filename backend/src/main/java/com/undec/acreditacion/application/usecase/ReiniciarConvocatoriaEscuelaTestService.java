package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ReiniciarConvocatoriaEscuelaTestUseCase;
import com.undec.acreditacion.domain.entities.Convocatoria;
import com.undec.acreditacion.domain.entities.ConvocatoriaCarrera;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ReiniciarConvocatoriaEscuelaTestService implements ReiniciarConvocatoriaEscuelaTestUseCase {

    private final ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public ReiniciarConvocatoriaEscuelaTestService(
            ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository) {
        this.convocatoriaRepository = Objects.requireNonNull(convocatoriaRepository, "convocatoriaRepository no puede ser nulo");
        this.procesoRepository = Objects.requireNonNull(procesoRepository, "procesoRepository no puede ser nulo");
    }

    @Override
    public Convocatoria reiniciar(UUID convocatoriaId) {
        if (convocatoriaId == null) {
            throw new DomainValidationException("El ID de la convocatoria es requerido");
        }

        Convocatoria convocatoria = convocatoriaRepository.findById(convocatoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la convocatoria con ID: " + convocatoriaId));

        List<UUID> procesosAEliminar = convocatoria.getCarreras().stream()
                .map(ConvocatoriaCarrera::getProcesoId)
                .filter(Objects::nonNull)
                .toList();

        // 1. Resetear convocatoria y desvincular procesos de las carreras
        convocatoria.reiniciarParaPruebas();
        Convocatoria guardada = convocatoriaRepository.save(convocatoria);

        // 2. Eliminar procesos y sus datos asociados en cascada
        for (UUID procesoId : procesosAEliminar) {
            procesoRepository.deleteById(procesoId);
        }

        return guardada;
    }
}
