package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarRegistroCaUseCase;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
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
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> {
                    RegistroConformacionCa nueva = new RegistroConformacionCa(null, procesoId);
                    nueva.solicitar(proceso.getCreadoPor(), 15);
                    RegistroConformacionCa saved = registroCaRepository.save(nueva);
                    return saved != null ? saved : nueva;
                });
        if (ca.getFechaLimiteDc() == null) {
            ca.solicitar(proceso.getCreadoPor(), 15);
            RegistroConformacionCa saved = registroCaRepository.save(ca);
            if (saved != null) {
                ca = saved;
            }
        }

        return RegistroCaMapper.toDto(ca);
    }
}
