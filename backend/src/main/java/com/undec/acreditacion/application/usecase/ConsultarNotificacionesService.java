package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarNotificacionesUseCase;
import com.undec.acreditacion.application.input.CrearSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.output.DatosPayloadAltaMiembro;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.entities.SolicitudNotificacion;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import com.undec.acreditacion.domain.ports.SolicitudNotificacionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ConsultarNotificacionesService implements ConsultarNotificacionesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultarNotificacionesService.class);

    private final SolicitudNotificacionRepositoryPort repository;
    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase;

    public ConsultarNotificacionesService(SolicitudNotificacionRepositoryPort repository) {
        this(repository, null, null, null, null);
    }

    @Autowired
    public ConsultarNotificacionesService(
            SolicitudNotificacionRepositoryPort repository,
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CarreraRepositoryPort carreraRepository,
            CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase
    ) {
        this.repository = repository;
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.carreraRepository = carreraRepository;
        this.crearSolicitudNotificacionUseCase = crearSolicitudNotificacionUseCase;
    }

    @Override
    public List<SolicitudNotificacionDto> listar(String rol, String estado) {
        sincronizarPendientesDesdeComisiones();
        return repository.listar(rol, estado).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public long contarPendientes(String rol) {
        sincronizarPendientesDesdeComisiones();
        return repository.contarPendientes(rol);
    }

    private void sincronizarPendientesDesdeComisiones() {
        if (registroCaRepository == null || crearSolicitudNotificacionUseCase == null) {
            return;
        }
        try {
            List<RegistroConformacionCa> registros = registroCaRepository.findAll();
            for (RegistroConformacionCa ca : registros) {
                if (ca.getMiembros() == null || ca.getMiembros().isEmpty()) {
                    continue;
                }
                String carreraNombre = null;
                for (MiembroComisionAutoevaluacion m : ca.getMiembros()) {
                    if (m.isPendienteAltaSistema()) {
                        if (carreraNombre == null) {
                            carreraNombre = resolverNombreCarrera(ca);
                        }
                        String doc = m.getDniCuil();
                        String cuit = (m.getCuitPasaporte() != null && !m.getCuitPasaporte().isBlank())
                                ? m.getCuitPasaporte()
                                : doc;
                        DatosPayloadAltaMiembro payload = new DatosPayloadAltaMiembro(
                                m.getApellido(),
                                m.getNombreCompleto(),
                                doc,
                                cuit,
                                m.getCorreo(),
                                m.getCorreoGmail(),
                                m.getCargoFuncion()
                        );
                        crearSolicitudNotificacionUseCase.registrarSolicitudAltaMiembro(
                                ca.getProcesoId(),
                                ca.getRemitidoPor(),
                                "Director de Carrera",
                                carreraNombre,
                                payload
                        );
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo sincronizar miembros pendientes de alta en comisiones: {}", e.getMessage());
        }
    }

    private String resolverNombreCarrera(RegistroConformacionCa ca) {
        if (procesoRepository != null && carreraRepository != null && ca.getProcesoId() != null) {
            return procesoRepository.findById(ca.getProcesoId())
                    .filter(p -> p.getCarreraId() != null)
                    .flatMap(p -> carreraRepository.findById(p.getCarreraId()))
                    .map(Carrera::getNombre)
                    .orElse("Carrera de Grado");
        }
        return "Carrera de Grado";
    }

    public SolicitudNotificacionDto toDto(SolicitudNotificacion s) {
        if (s == null) return null;
        return new SolicitudNotificacionDto(
                s.getId(),
                s.getTipo(),
                s.getTitulo(),
                s.getMensaje(),
                s.getDatosPayload(),
                s.getProcesoId(),
                s.getCarreraNombre(),
                s.getSolicitadoPor(),
                s.getSolicitadoPorNombre(),
                s.getDestinatarioRol(),
                s.getEstado(),
                s.isLeida(),
                s.getResueltoPor(),
                s.getResueltoEn(),
                s.getCreadoEn()
        );
    }
}
