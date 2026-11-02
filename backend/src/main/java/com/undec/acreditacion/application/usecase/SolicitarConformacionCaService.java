package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.SolicitarConformacionCaUseCase;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.ProcesoAcreditacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.entities.RegistroConvocatoria;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;

import java.util.UUID;

public class SolicitarConformacionCaService implements SolicitarConformacionCaUseCase {

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;

    public SolicitarConformacionCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CarreraRepositoryPort carreraRepository,
            RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.carreraRepository = carreraRepository;
        this.registroConvocatoriaRepository = registroConvocatoriaRepository;
        this.planTrabajoRepository = planTrabajoRepository;
    }

    @Override
    public RegistroCaDto solicitar(UUID procesoId, UUID usuarioAeaId) {
        return solicitar(procesoId, usuarioAeaId, 15);
    }

    @Override
    public RegistroCaDto solicitar(UUID procesoId, UUID usuarioAeaId, Integer diasPlazo) {
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        Carrera carrera = carreraRepository.findById(proceso.getCarreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera vinculada al proceso"));

        if (!Carrera.NIVEL_GRADO.equalsIgnoreCase(carrera.getNivel())) {
            throw new DomainValidationException("La conformación de Comisión de Autoevaluación aplica exclusivamente a carreras de nivel GRADO");
        }

        // Precondición: RG-001 y Plan de Trabajo deben estar formalmente finalizados
        RegistroConvocatoria regConv = registroConvocatoriaRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new DomainValidationException("El registro RG-001 no existe para el proceso"));
        if (!regConv.isEnviado()) {
            throw new DomainValidationException("No se puede solicitar la conformación de la CA: el formulario RG-001 debe estar formalmente enviado");
        }

        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new DomainValidationException("El Plan de Trabajo (PT-001) no existe para el proceso"));
        if (!plan.isEnviado()) {
            throw new DomainValidationException("No se puede solicitar la conformación de la CA: el Plan de Trabajo debe estar formalmente enviado");
        }

        boolean act2Completa = plan.getActividades().stream()
                .anyMatch(a -> ActividadPlanTrabajo.CODIGO_ACT_02.equals(a.getCodigoActividad())
                        && a.getEstado() == com.undec.acreditacion.domain.entities.EstadoActividad.COMPLETADA);
        if (!act2Completa) {
            throw new DomainValidationException("No se puede solicitar la conformación de la CA: la actividad de Elaboración del Plan de Trabajo (RG-001) debe estar finalizada");
        }

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> new RegistroConformacionCa(null, procesoId));

        ca.solicitar(usuarioAeaId, diasPlazo);
        RegistroConformacionCa guardado = registroCaRepository.save(ca);

        return RegistroCaMapper.toDto(guardado);
    }

    @Override
    public RegistroCaDto extenderPlazoFecha(UUID procesoId, UUID usuarioAeaId, java.time.LocalDate nuevaFechaLimite) {
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(proceso.getId())
                .orElseGet(() -> new RegistroConformacionCa(null, proceso.getId()));

        ca.extenderPlazoFecha(usuarioAeaId, nuevaFechaLimite);
        RegistroConformacionCa guardado = registroCaRepository.save(ca);

        planTrabajoRepository.findByProcesoId(procesoId).ifPresent(plan -> {
            for (ActividadPlanTrabajo act : plan.getActividades()) {
                String cod = act.getCodigoActividad() != null ? act.getCodigoActividad().trim() : "";
                String nom = act.getNombre() != null ? act.getNombre().toLowerCase() : "";
                if ("3.1".equals(cod) || "3.2".equals(cod) || nom.contains("comisión de autoevaluación")) {
                    act.agregarPlazoExtendido(nuevaFechaLimite);
                    planTrabajoRepository.saveActividad(act);
                }
            }
        });

        return RegistroCaMapper.toDto(guardado);
    }
}
