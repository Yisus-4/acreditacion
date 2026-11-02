package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarRegistroUsuariosAteneaUseCase;
import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroUsuariosAteneaRepositoryPort;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

public class ConsultarRegistroUsuariosAteneaService implements ConsultarRegistroUsuariosAteneaUseCase {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final RegistroUsuariosAteneaRepositoryPort registroUsuariosRepository;
    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;

    public ConsultarRegistroUsuariosAteneaService(
            RegistroUsuariosAteneaRepositoryPort registroUsuariosRepository,
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository
    ) {
        this.registroUsuariosRepository = registroUsuariosRepository;
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
    }

    @Override
    public RegistroUsuariosAteneaDto consultarPorProcesoId(UUID procesoId) {
        Optional<RegistroUsuariosAtenea> existente = registroUsuariosRepository.findByProcesoId(procesoId);
        if (existente.isPresent()) {
            RegistroUsuariosAtenea reg = existente.get();
            if (reg.getMatrizPermisos().isEmpty() && reg.getEstado() != EstadoRegistroUsuariosAtenea.FINALIZADO) {
                reg.inicializarMatrizPorDefectoSiVacia();
                reg = registroUsuariosRepository.save(reg);
            }
            return RegistroUsuariosAteneaMapper.toDto(reg);
        }

        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        Optional<RegistroConformacionCa> caOpt = registroCaRepository.findByProcesoId(procesoId);

        LocalDate fechaInicio = LocalDate.now();
        if (caOpt.isPresent()) {
            if (caOpt.get().getAprobadoEn() != null) {
                fechaInicio = LocalDate.ofInstant(caOpt.get().getAprobadoEn(), ZONA_ARGENTINA);
            } else if (caOpt.get().getRemitidoEn() != null) {
                fechaInicio = LocalDate.ofInstant(caOpt.get().getRemitidoEn(), ZONA_ARGENTINA);
            }
        }

        RegistroUsuariosAtenea nuevo = RegistroUsuariosAtenea.iniciar(procesoId, proceso.getCreadoPor(), fechaInicio);

        // Pre-poblar usuarios de AEA
        nuevo.agregarUsuario(UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.AEA,
                "Responsable Técnico",
                "AEA",
                "00-00000000-0",
                "aea@undec.edu.ar",
                "Área de Evaluación y Acreditación (AEA)",
                "Equipo Técnico AEA",
                TipoAccesoAtenea.ADMINISTRACION,
                "Gestión Integral y Administración CONEAU Global ATENEA",
                false,
                true,
                true
        ));

        // Pre-poblar usuarios de SGI
        nuevo.agregarUsuario(UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.SGI,
                "Responsable",
                "SGI",
                "00-00000001-1",
                "sgi@undec.edu.ar",
                "Secretaría de Gestión Institucional",
                "Secretario / Responsable SGI",
                TipoAccesoAtenea.VISUALIZACION,
                "Datos Institucionales y Autoridades",
                false,
                true,
                true
        ));

        // Pre-poblar miembros de la CA si existe
        if (caOpt.isPresent()) {
            nuevo.importarMiembrosCa(caOpt.get().getMiembros());
        }

        RegistroUsuariosAtenea guardado = registroUsuariosRepository.save(nuevo);
        return RegistroUsuariosAteneaMapper.toDto(guardado);
    }
}
