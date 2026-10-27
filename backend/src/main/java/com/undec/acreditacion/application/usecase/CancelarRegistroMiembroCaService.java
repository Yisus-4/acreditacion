package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.CancelarRegistroMiembroCaUseCase;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CancelarRegistroMiembroCaService implements CancelarRegistroMiembroCaUseCase {

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public CancelarRegistroMiembroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
    }

    @Override
    @Transactional
    public RegistroCaDto cancelarRegistro(UUID procesoId, UUID miembroId, UUID usuarioId) {
        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proceso de acreditación no encontrado con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró registro de conformación de CA para el proceso: " + procesoId));

        MiembroComisionAutoevaluacion miembro = ca.getMiembros().stream()
                .filter(m -> m.getId().equals(miembroId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el miembro con ID: " + miembroId + " en la comisión"));

        // Se descarta el alta permanente en el catálogo, dejándolo solo para esta convocatoria
        miembro.setPendienteAltaSistema(false);
        RegistroConformacionCa saved = registroCaRepository.save(ca);
        return RegistroCaMapper.toDto(saved);
    }
}
