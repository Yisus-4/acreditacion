package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ObservarRegistroCaUseCase;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;

import java.util.UUID;

public class ObservarRegistroCaService implements ObservarRegistroCaUseCase {

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public ObservarRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
    }

    @Override
    public RegistroCaDto observar(UUID procesoId, UUID usuarioAeaId, String motivo) {
        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new DomainValidationException("No existe registro de conformación de CA para el proceso"));

        ca.observar(usuarioAeaId, motivo);
        RegistroConformacionCa guardado = registroCaRepository.save(ca);

        return RegistroCaMapper.toDto(guardado);
    }
}
