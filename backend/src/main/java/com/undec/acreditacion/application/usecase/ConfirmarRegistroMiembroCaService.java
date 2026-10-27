package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConfirmarRegistroMiembroCaUseCase;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.CatalogoMiembroComision;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CatalogoMiembroComisionRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConfirmarRegistroMiembroCaService implements ConfirmarRegistroMiembroCaUseCase {

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CatalogoMiembroComisionRepositoryPort catalogoRepository;

    public ConfirmarRegistroMiembroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CatalogoMiembroComisionRepositoryPort catalogoRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.catalogoRepository = catalogoRepository;
    }

    @Override
    @Transactional
    public RegistroCaDto confirmarRegistro(UUID procesoId, UUID miembroId, UUID usuarioId) {
        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proceso de acreditación no encontrado con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró registro de conformación de CA para el proceso: " + procesoId));

        MiembroComisionAutoevaluacion miembro = ca.getMiembros().stream()
                .filter(m -> m.getId().equals(miembroId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el miembro con ID: " + miembroId + " en la comisión"));

        String doc = miembro.getDniCuil();
        String cuit = miembro.getCuitPasaporte() != null && !miembro.getCuitPasaporte().isBlank()
                ? miembro.getCuitPasaporte()
                : doc;
        String correo = miembro.getCorreo();
        String correoGmail = miembro.getCorreoGmail() != null && !miembro.getCorreoGmail().isBlank()
                ? miembro.getCorreoGmail()
                : correo;
        String cargo = miembro.getCargoFuncion() != null && !miembro.getCargoFuncion().isBlank()
                ? miembro.getCargoFuncion()
                : "Docente";

        Optional<CatalogoMiembroComision> existente = catalogoRepository.findByDocumento(doc);
        if (existente.isEmpty() && cuit != null && !cuit.isBlank()) {
            existente = catalogoRepository.findByCuitPasaporte(cuit);
        }

        if (existente.isEmpty()) {
            CatalogoMiembroComision nuevo = new CatalogoMiembroComision(
                    UUID.randomUUID(),
                    miembro.getApellido(),
                    miembro.getNombreCompleto(),
                    doc,
                    cuit,
                    correo,
                    correoGmail,
                    cargo,
                    miembro.isEsDocente(),
                    Instant.now()
            );
            catalogoRepository.save(nuevo);
        }

        miembro.setPendienteAltaSistema(false);
        RegistroConformacionCa saved = registroCaRepository.save(ca);
        return RegistroCaMapper.toDto(saved);
    }
}
