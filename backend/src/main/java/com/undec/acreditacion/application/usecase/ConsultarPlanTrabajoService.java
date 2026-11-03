package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarPlanTrabajoUseCase;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.EstadoActividad;
import com.undec.acreditacion.domain.entities.EstadoRegistroCa;
import com.undec.acreditacion.domain.entities.EstadoRegistroUsuariosAtenea;
import com.undec.acreditacion.domain.entities.HojaPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroUsuariosAteneaRepositoryPort;
import com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ConsultarPlanTrabajoService implements ConsultarPlanTrabajoUseCase {

    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private SolicitudNormativaRepositoryPort solicitudNormativaRepository;
    private RegistroConformacionCaRepositoryPort registroCaRepository;
    private RegistroUsuariosAteneaRepositoryPort registroUsuariosAteneaRepository;

    public ConsultarPlanTrabajoService(PlanTrabajoRepositoryPort planTrabajoRepository) {
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
    }

    public ConsultarPlanTrabajoService(
            PlanTrabajoRepositoryPort planTrabajoRepository,
            SolicitudNormativaRepositoryPort solicitudNormativaRepository,
            RegistroConformacionCaRepositoryPort registroCaRepository,
            RegistroUsuariosAteneaRepositoryPort registroUsuariosAteneaRepository
    ) {
        this.planTrabajoRepository = Objects.requireNonNull(planTrabajoRepository);
        this.solicitudNormativaRepository = solicitudNormativaRepository;
        this.registroCaRepository = registroCaRepository;
        this.registroUsuariosAteneaRepository = registroUsuariosAteneaRepository;
    }

    @Override
    public PlanTrabajo obtenerPorProcesoId(UUID procesoId) {
        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de trabajo para el proceso: " + procesoId));

        boolean modificado = false;

        boolean tieneActividad13 = plan.getActividades().stream().anyMatch(PlanTrabajo::esActividadNormativa);
        if (!tieneActividad13) {
            ActividadPlanTrabajo actividad13 = ActividadPlanTrabajo.crearActividadNormativa(plan.getId());
            plan.agregarActividad(actividad13);
            persistirActividadAgregada(plan, actividad13.getId());
            modificado = true;
        }

        boolean tieneActividad04 = plan.getActividades().stream().anyMatch(PlanTrabajo::esActividadIdentificarDocumentacionInteres);
        if (!tieneActividad04) {
            ActividadPlanTrabajo actividad04 = ActividadPlanTrabajo.crearActividadIdentificarDocumentacionInteres(plan.getId());
            plan.agregarActividad(actividad04);
            persistirActividadAgregada(plan, actividad04.getId());
            modificado = true;
        }

        boolean tieneActividad06 = plan.getActividades().stream().anyMatch(PlanTrabajo::esActividadFormalizacionElectronica);
        if (!tieneActividad06) {
            ActividadPlanTrabajo actividad06 = ActividadPlanTrabajo.crearActividadFormalizacionElectronica(plan.getId());
            plan.agregarActividad(actividad06);
            persistirActividadAgregada(plan, actividad06.getId());
            modificado = true;
        }

        boolean tieneEtapasPosteriores = (registroCaRepository != null && registroCaRepository.findByProcesoId(procesoId).isPresent())
                || (registroUsuariosAteneaRepository != null && registroUsuariosAteneaRepository.findByProcesoId(procesoId).isPresent())
                || plan.isEnviado();

        boolean normativaCompletada = plan.getActividades().stream()
                .filter(PlanTrabajo::esActividadNormativa)
                .anyMatch(a -> a.getEstado() == EstadoActividad.COMPLETADA);

        // 1. Sincronizar Actividad 1 (Informar proceso) solo si ya se completó normativa o etapas posteriores
        if (normativaCompletada || tieneEtapasPosteriores) {
            plan.getActividades().stream()
                    .filter(a -> ActividadPlanTrabajo.CODIGO_ACT_01.equals(a.getCodigoActividad())
                            || (a.getNombre() != null && a.getNombre().toLowerCase().contains("informar sobre el proceso")))
                    .forEach(a -> {
                        if (a.getEstado() != EstadoActividad.COMPLETADA) {
                            a.marcarCompletadaAutomatica("Informado sobre el proceso de acreditación");
                            planTrabajoRepository.saveActividad(a);
                        }
                    });
        }

        // 1.b. Sincronizar Actividad 2 (Elaborar Plan de Trabajo) si ya se remitieron etapas posteriores o el plan fue enviado
        if (tieneEtapasPosteriores) {
            plan.getActividades().stream()
                    .filter(a -> ActividadPlanTrabajo.CODIGO_ACT_02.equals(a.getCodigoActividad())
                            || (a.getNombre() != null && a.getNombre().toLowerCase().contains("elaborar plan de trabajo")))
                    .forEach(a -> {
                        if (a.getEstado() != EstadoActividad.COMPLETADA) {
                            a.marcarCompletadaAutomatica("Cronograma institucional y formulario RG-001 formulados y vigentes");
                            planTrabajoRepository.saveActividad(a);
                        }
                    });
        }

        // 2. Sincronizar Actividad 1.4 / ACT-04 si Normativa ya está aprobada / completada
        if (normativaCompletada) {
            plan.getActividades().stream()
                    .filter(PlanTrabajo::esActividadIdentificarDocumentacionInteres)
                    .forEach(a -> {
                        if (a.getEstado() != EstadoActividad.COMPLETADA) {
                            a.marcarCompletadaAutomatica("Documentación de interés confirmada y aprobada con la normativa de carrera");
                            planTrabajoRepository.saveActividad(a);
                        }
                    });
        }

        // 3. Sincronizar Actividades 3.1 y 3.2 si Comisión de Autoevaluación ya fue aprobada o tiene resolución
        if (registroCaRepository != null) {
            registroCaRepository.findByProcesoId(procesoId).ifPresent(ca -> {
                boolean aprobada = ca.getEstado() == EstadoRegistroCa.APROBADA;
                boolean tieneResolucion = ca.getResolucionStoragePath() != null && !ca.getResolucionStoragePath().isBlank();
                boolean enviada = ca.getEstado() == EstadoRegistroCa.REMITIDA_A_AEA;

                plan.getActividades().stream()
                        .filter(a -> a.getHoja() == HojaPlanTrabajo.INFO)
                        .forEach(a -> {
                            boolean es31 = "3.1".equals(a.getCodigoActividad()) || (a.getNombre() != null && a.getNombre().toLowerCase().contains("solicitar la conformación"));
                            boolean es32 = "3.2".equals(a.getCodigoActividad()) || (a.getNombre() != null && a.getNombre().toLowerCase().contains("formalizar la conformación"));

                            if (es31 && (aprobada || tieneResolucion || enviada)) {
                                if (a.getEstado() != EstadoActividad.COMPLETADA) {
                                    a.marcarCompletadaAutomatica("Comisión de autoevaluación solicitada y remitida");
                                    planTrabajoRepository.saveActividad(a);
                                }
                            }
                            if (es32 && (aprobada || tieneResolucion)) {
                                if (a.getEstado() != EstadoActividad.COMPLETADA) {
                                    a.marcarCompletadaAutomatica("Formalización de la conformación de CA finalizada por AEA con resolución");
                                    planTrabajoRepository.saveActividad(a);
                                }
                            } else if (es32 && enviada && !tieneResolucion && a.getEstado() == EstadoActividad.PENDIENTE) {
                                a.cambiarEstado(EstadoActividad.EN_PROGRESO);
                                planTrabajoRepository.saveActividad(a);
                            }
                        });
            });
        }

        // 4. Sincronizar Actividad 3.3 si RegistroUsuariosAtenea ya fue finalizado
        if (registroUsuariosAteneaRepository != null) {
            registroUsuariosAteneaRepository.findByProcesoId(procesoId).ifPresent(ru -> {
                if (ru.getEstado() == EstadoRegistroUsuariosAtenea.FINALIZADO) {
                    plan.getActividades().stream()
                            .filter(a -> a.getHoja() == HojaPlanTrabajo.INFO
                                    && ("3.3".equals(a.getCodigoActividad())
                                    || (a.getNombre() != null && a.getNombre().toLowerCase().contains("definir usuarios"))
                                    || (a.getNombre() != null && a.getNombre().toLowerCase().contains("usuarios y permisos"))))
                            .forEach(a -> {
                                if (a.getEstado() != EstadoActividad.COMPLETADA) {
                                    a.marcarCompletadaAutomatica("Usuarios y permisos de acceso en ATENEA formalizados (RG-003)");
                                    planTrabajoRepository.saveActividad(a);
                                }
                            });
                }
            });
        }

        if (modificado) {
            planTrabajoRepository.save(plan);
        }

        return plan;
    }

    private void persistirActividadAgregada(PlanTrabajo plan, UUID actividadId) {
        plan.getActividades().stream()
                .filter(a -> a.getId().equals(actividadId))
                .findFirst()
                .ifPresent(planTrabajoRepository::saveActividad);
    }
}
