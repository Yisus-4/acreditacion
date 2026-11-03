package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.EliminarConvocatoriaUseCase;
import com.undec.acreditacion.domain.entities.Convocatoria;
import com.undec.acreditacion.domain.entities.ConvocatoriaCarrera;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class EliminarConvocatoriaService implements EliminarConvocatoriaUseCase {

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final ConvocatoriaEscuelaRepositoryPort convocatoriaEscuelaRepository;

    public EliminarConvocatoriaService(ProcesoAcreditacionRepositoryPort procesoRepository) {
        this(procesoRepository, null);
    }

    public EliminarConvocatoriaService(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            ConvocatoriaEscuelaRepositoryPort convocatoriaEscuelaRepository
    ) {
        this.procesoRepository = Objects.requireNonNull(procesoRepository, "El repositorio de procesos de acreditación es obligatorio");
        this.convocatoriaEscuelaRepository = convocatoriaEscuelaRepository;
    }

    @Override
    public void ejecutar(UUID procesoId) {
        Objects.requireNonNull(procesoId, "El ID del proceso de acreditación es obligatorio");

        if (convocatoriaEscuelaRepository != null) {
            Optional<Convocatoria> convOpt = convocatoriaEscuelaRepository.findById(procesoId);
            if (convOpt.isPresent()) {
                Convocatoria conv = convOpt.get();
                List<UUID> procesosIds = conv.getCarreras().stream()
                        .map(ConvocatoriaCarrera::getProcesoId)
                        .filter(Objects::nonNull)
                        .toList();
                convocatoriaEscuelaRepository.deleteById(procesoId);
                for (UUID pid : procesosIds) {
                    procesoRepository.findById(pid).ifPresent(p -> procesoRepository.deleteById(pid));
                }
                return;
            }
        }

        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        procesoRepository.deleteById(procesoId);
    }
}
