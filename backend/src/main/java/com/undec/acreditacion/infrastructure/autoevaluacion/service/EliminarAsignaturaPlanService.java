package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EliminarAsignaturaPlanService {

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;
    private final SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionCarreraAmbitoRepository;
    private final SpringDataAutoevaluacionAmbitoActividadRepository autoevaluacionAmbitoActividadRepository;
    private final SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;

    public EliminarAsignaturaPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionCarreraAmbitoRepository
    ) {
        this(planRepository, asignaturaRepository, autoevaluacionCarreraAmbitoRepository, null, null);
    }

    public EliminarAsignaturaPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionCarreraAmbitoRepository,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService
    ) {
        this(planRepository, asignaturaRepository, autoevaluacionCarreraAmbitoRepository, sincronizacionBidireccionalPlanService, null);
    }

    @Autowired
    public EliminarAsignaturaPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionCarreraAmbitoRepository,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService,
            SpringDataAutoevaluacionAmbitoActividadRepository autoevaluacionAmbitoActividadRepository
    ) {
        this.planRepository = planRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.autoevaluacionCarreraAmbitoRepository = autoevaluacionCarreraAmbitoRepository;
        this.sincronizacionBidireccionalPlanService = sincronizacionBidireccionalPlanService;
        this.autoevaluacionAmbitoActividadRepository = autoevaluacionAmbitoActividadRepository;
    }

    @Transactional
    public void ejecutar(UUID planId, UUID asigId) {
        eliminarAsignatura(planId, asigId);
    }

    @Transactional
    public void eliminarAsignatura(UUID planId, UUID asigId) {
        PlanEstudioFt001JpaEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio con ID: " + planId));

        ActividadCurricularPlanJpaEntity asig = asignaturaRepository.findById(asigId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignatura con ID: " + asigId));

        if (!asig.getPlanEstudio().getId().equals(planId)) {
            throw new DomainValidationException("La asignatura no pertenece al plan de estudio indicado.");
        }

        List<ActividadCurricularPlanJpaEntity> asignaturasDelPlan =
                asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId);
        if (asignaturasDelPlan == null) {
            asignaturasDelPlan = List.of();
        }

        boolean existeOtraConMismoNombre = asignaturasDelPlan.stream()
                .anyMatch(otra -> otra.getId() != null
                        && !otra.getId().equals(asigId)
                        && !otra.getEstadoDictado().esInactiva()
                        && otra.getNombre() != null
                        && asig.getNombre() != null
                        && otra.getNombre().trim().equalsIgnoreCase(asig.getNombre().trim()));

        UUID planCarreraId = plan.getCarrera() != null ? plan.getCarrera().getId() : null;

        // 1a. Guard relacional exacto por UUID de actividad curricular en ámbitos activos del proceso/carrera
        if (plan.getProcesoId() != null && autoevaluacionAmbitoActividadRepository != null) {
            List<AutoevaluacionAmbitoActividadJpaEntity> vinculosExactos =
                    autoevaluacionAmbitoActividadRepository.findActiveUsagesByActividadAndProceso(asigId, plan.getProcesoId());
            if (vinculosExactos != null) {
                for (AutoevaluacionAmbitoActividadJpaEntity link : vinculosExactos) {
                    AutoevaluacionCarreraAmbitoJpaEntity vinculo = link.getAutoevaluacionAmbito();
                    if (correspondeACarreraYProceso(vinculo, plan.getProcesoId(), planCarreraId)) {
                        String nombreAmbito = vinculo.getAmbito().getDenominacion() != null
                                ? vinculo.getAmbito().getDenominacion()
                                : "Ámbito de práctica";
                        throw new BusinessRuleConflictException(
                                "No es posible eliminar la asignatura porque se encuentra vinculada a los ámbitos de práctica: " + nombreAmbito
                        );
                    }
                }
            }
        }

        // 1b. Guard de respaldo sobre texto legado/manual únicamente en ámbitos clonados válidos del proceso/carrera
        if (plan.getProcesoId() != null) {
            List<AutoevaluacionCarreraAmbitoJpaEntity> ambitos =
                    autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(plan.getProcesoId());

            if (ambitos != null) {
                for (AutoevaluacionCarreraAmbitoJpaEntity vinculo : ambitos) {
                    if (!correspondeACarreraYProceso(vinculo, plan.getProcesoId(), planCarreraId)) {
                        continue;
                    }
                    if (vinculo.getAsignaturasVinculadas() != null && !vinculo.getAsignaturasVinculadas().isBlank()) {
                        if (contieneTokenAsignatura(vinculo.getAsignaturasVinculadas(), asig, existeOtraConMismoNombre)) {
                            String nombreAmbito = vinculo.getAmbito().getDenominacion() != null
                                    ? vinculo.getAmbito().getDenominacion()
                                    : "Ámbito de práctica";
                            throw new BusinessRuleConflictException(
                                    "No es posible eliminar la asignatura porque se encuentra vinculada a los ámbitos de práctica: " + nombreAmbito
                            );
                        }
                    }
                }
            }
        }

        // 2. Guard contra correlatividades descendentes en el mismo plan (Token-matching)
        for (ActividadCurricularPlanJpaEntity otra : asignaturasDelPlan) {
            if (otra.getId() != null && !otra.getId().equals(asigId)
                    && otra.getCorrelativas() != null && !otra.getCorrelativas().isBlank()) {
                if (contieneTokenAsignatura(otra.getCorrelativas(), asig, existeOtraConMismoNombre)) {
                    throw new BusinessRuleConflictException(
                            "No es posible eliminar la asignatura porque es correlativa de: " + otra.getNombre()
                    );
                }
            }
        }

        // 3. Eliminar asignatura, recalcular horas y propagar bidireccionalmente
        asignaturaRepository.delete(asig);

        recalcularHorasPlan(plan);

        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarEliminacionAsignatura(plan, asig);
        }
    }

    private boolean correspondeACarreraYProceso(
            AutoevaluacionCarreraAmbitoJpaEntity vinculo,
            UUID procesoId,
            UUID planCarreraId
    ) {
        if (vinculo == null || !Boolean.TRUE.equals(vinculo.getUtilizadoPorCarrera())) {
            return false;
        }
        AmbitoPracticaJpaEntity ambito = vinculo.getAmbito();
        if (ambito == null
                || !Boolean.TRUE.equals(ambito.getActivo())
                || Boolean.TRUE.equals(ambito.getEsMaestro())
                || ambito.getProcesoId() == null
                || !procesoId.equals(ambito.getProcesoId())) {
            return false;
        }
        if (planCarreraId != null && vinculo.getCarreraId() != null && !planCarreraId.equals(vinculo.getCarreraId())) {
            return false;
        }
        return true;
    }

    private boolean contieneTokenAsignatura(
            String textoDelimitado,
            ActividadCurricularPlanJpaEntity asig,
            boolean existeOtraConMismoNombre
    ) {
        String codigo = asig.getCodigo() != null ? asig.getCodigo().trim() : null;
        String nombre = asig.getNombre() != null ? asig.getNombre().trim() : null;

        String[] tokens = textoDelimitado.split("[,;]");
        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) continue;

            if (asig.getId() != null && token.equalsIgnoreCase(asig.getId().toString())) {
                return true;
            }
            if (codigo != null && !codigo.isEmpty() && token.equalsIgnoreCase(codigo)) {
                return true;
            }
            if (token.endsWith("]")) {
                int openIdx = token.lastIndexOf('[');
                if (openIdx >= 0) {
                    String bracketCode = token.substring(openIdx + 1, token.length() - 1).trim();
                    if (!bracketCode.isEmpty()) {
                        if (codigo != null && !codigo.isEmpty() && bracketCode.equalsIgnoreCase(codigo)) {
                            return true;
                        }
                        continue;
                    }
                }
            }
            if (!existeOtraConMismoNombre && nombre != null && !nombre.isEmpty() && token.equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }

    private void recalcularHorasPlan(PlanEstudioFt001JpaEntity plan) {
        List<ActividadCurricularPlanJpaEntity> asignaturas = asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(plan.getId());
        int presenciales = 0;
        int noPresenciales = 0;
        int totales = 0;

        for (ActividadCurricularPlanJpaEntity a : asignaturas) {
            int pres = a.getHorasPresenciales() != null ? a.getHorasPresenciales() : 0;
            int noPres = a.getHorasNoPresenciales() != null ? a.getHorasNoPresenciales() : 0;
            int reloj = a.getHorasReloj() != null ? a.getHorasReloj() : 0;

            presenciales += pres;
            noPresenciales += noPres;
            totales += (pres + noPres > 0 ? (pres + noPres) : reloj);
        }

        plan.setHorasPresenciales(presenciales);
        plan.setHorasNoPresenciales(noPresenciales);
        plan.setHorasTotales(totales);
        plan.setActualizadoEn(Instant.now());
        planRepository.save(plan);
    }
}
