package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
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

    public EliminarAsignaturaPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionCarreraAmbitoRepository
    ) {
        this.planRepository = planRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.autoevaluacionCarreraAmbitoRepository = autoevaluacionCarreraAmbitoRepository;
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

        // 1. Guard contra vinculaciones activas en ámbitos de práctica (Token-matching)
        if (plan.getProcesoId() != null) {
            List<AutoevaluacionCarreraAmbitoJpaEntity> ambitos =
                    autoevaluacionCarreraAmbitoRepository.findByProcesoIdAndUtilizadoPorCarreraTrue(plan.getProcesoId());

            for (AutoevaluacionCarreraAmbitoJpaEntity vinculo : ambitos) {
                if (vinculo.getAsignaturasVinculadas() != null && !vinculo.getAsignaturasVinculadas().isBlank()) {
                    if (contieneTokenAsignatura(vinculo.getAsignaturasVinculadas(), asig)) {
                        String nombreAmbito = vinculo.getAmbito() != null && vinculo.getAmbito().getDenominacion() != null ?
                                vinculo.getAmbito().getDenominacion() : "Ámbito de práctica";
                        throw new BusinessRuleConflictException(
                                "No es posible eliminar la asignatura porque se encuentra vinculada a los ámbitos de práctica: " + nombreAmbito
                        );
                    }
                }
            }
        }

        // 2. Guard contra correlatividades descendentes en el mismo plan (Token-matching)
        List<ActividadCurricularPlanJpaEntity> asignaturasDelPlan =
                asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId);

        for (ActividadCurricularPlanJpaEntity otra : asignaturasDelPlan) {
            if (!otra.getId().equals(asigId) && otra.getCorrelativas() != null && !otra.getCorrelativas().isBlank()) {
                if (contieneTokenAsignatura(otra.getCorrelativas(), asig)) {
                    throw new BusinessRuleConflictException(
                            "No es posible eliminar la asignatura porque es correlativa de: " + otra.getNombre()
                    );
                }
            }
        }

        // 3. Eliminar asignatura y recalcular horas
        asignaturaRepository.delete(asig);

        recalcularHorasPlan(plan);
    }

    private boolean contieneTokenAsignatura(String textoDelimitado, ActividadCurricularPlanJpaEntity asig) {
        String[] tokens = textoDelimitado.split("[,;]");
        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) continue;

            if (token.equalsIgnoreCase(asig.getNombre())) {
                return true;
            }
            if (asig.getCodigo() != null && token.equalsIgnoreCase(asig.getCodigo())) {
                return true;
            }
            if (asig.getId() != null && token.equalsIgnoreCase(asig.getId().toString())) {
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
