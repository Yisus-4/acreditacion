package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarRegistroCaUseCase;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;

import java.util.UUID;

public class ConsultarRegistroCaService implements ConsultarRegistroCaUseCase {

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public ConsultarRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
    }

    @Override
    public RegistroCaDto consultarPorProcesoId(UUID procesoId) {
        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> new RegistroConformacionCa(null, procesoId));

        return RegistroCaMapper.toDto(ca);
    }
}
