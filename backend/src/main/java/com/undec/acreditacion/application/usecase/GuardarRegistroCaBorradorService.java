package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.GuardarRegistroCaBorradorUseCase;
import com.undec.acreditacion.application.input.MiembroCaInputDto;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;

import java.util.List;
import java.util.UUID;

public class GuardarRegistroCaBorradorService implements GuardarRegistroCaBorradorUseCase {

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public GuardarRegistroCaBorradorService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
    }

    @Override
    public RegistroCaDto guardarBorrador(UUID procesoId, boolean coincideCurricular, List<MiembroCaInputDto> miembros) {
        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> new RegistroConformacionCa(null, procesoId));

        List<MiembroComisionAutoevaluacion> listaMiembros = RegistroCaMapper.toDomainList(miembros);
        ca.guardarBorrador(coincideCurricular, listaMiembros);

        RegistroConformacionCa guardado = registroCaRepository.save(ca);
        return RegistroCaMapper.toDto(guardado);
    }
}
