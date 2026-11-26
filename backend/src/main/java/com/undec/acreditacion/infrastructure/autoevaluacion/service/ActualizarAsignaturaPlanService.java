package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActividadCurricularPlanDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActualizarAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ActualizarAsignaturaPlanService {

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;
    private final SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService;

    public ActualizarAsignaturaPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository
    ) {
        this(planRepository, asignaturaRepository, null);
    }

    @Autowired
    public ActualizarAsignaturaPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SincronizacionBidireccionalPlanService sincronizacionBidireccionalPlanService
    ) {
        this.planRepository = planRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.sincronizacionBidireccionalPlanService = sincronizacionBidireccionalPlanService;
    }

    @Transactional
    public ActividadCurricularPlanDto ejecutar(UUID planId, UUID asigId, ActualizarAsignaturaRequest request) {
        return actualizarAsignatura(planId, asigId, request);
    }

    @Transactional
    public ActividadCurricularPlanDto actualizarAsignatura(UUID planId, UUID asigId, ActualizarAsignaturaRequest request) {
        PlanEstudioFt001JpaEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio con ID: " + planId));

        ActividadCurricularPlanJpaEntity asig = asignaturaRepository.findById(asigId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignatura con ID: " + asigId));

        if (!asig.getPlanEstudio().getId().equals(planId)) {
            throw new DomainValidationException("La asignatura no pertenece al plan de estudio indicado.");
        }

        if (request.nombre() == null || request.nombre().trim().isEmpty()) {
            throw new DomainValidationException("El nombre de la asignatura es obligatorio.");
        }
        if (request.nombre().trim().length() > 255) {
            throw new DomainValidationException("El nombre de la asignatura no puede exceder los 255 caracteres.");
        }
        if (request.grupo() == null || request.grupo().trim().isEmpty()) {
            throw new DomainValidationException("El ciclo formativo (grupo) es obligatorio.");
        }
        if (request.subgrupo() == null || request.subgrupo().trim().isEmpty()) {
            throw new DomainValidationException("El año de cursada (subgrupo) es obligatorio.");
        }

        validarHoras(request.horasReloj(), request.horasPresenciales(), request.horasNoPresenciales(), request.horasSemanales());
        validarAutocorrelatividad(asig, request);
        validarCorrelativasConAsignaturasPrevias(planId, asigId, request.correlativas());

        if (request.codigo() != null && !request.codigo().trim().isEmpty()) {
            String nuevoCodigo = request.codigo().trim();
            if (!nuevoCodigo.equalsIgnoreCase(asig.getCodigo()) &&
                asignaturaRepository.existsByPlanEstudioIdAndCodigo(planId, nuevoCodigo)) {
                throw new DomainValidationException("Ya existe una asignatura con el código " + nuevoCodigo + " en este plan de estudio.");
            }
            asig.setCodigo(nuevoCodigo);
        } else {
            asig.setCodigo(null);
        }

        asig.setNombre(request.nombre().trim());
        asig.setGrupo(request.grupo());
        asig.setSubgrupo(request.subgrupo());
        asig.setHorasReloj(request.horasReloj());
        asig.setHorasPresenciales(request.horasPresenciales());
        asig.setHorasNoPresenciales(request.horasNoPresenciales());
        asig.setHorasSemanales(request.horasSemanales());
        asig.setCorrelativas(request.correlativas() != null && !request.correlativas().trim().isEmpty() ?
                request.correlativas().trim() : "Sin correlativas previas");
        if (request.esObligatoria() != null) {
            asig.setEsObligatoria(request.esObligatoria());
        }
        if (request.estadoDictado() != null) {
            asig.setEstadoDictado(request.estadoDictado());
        }
        if (request.numeroOrden() != null) {
            asig.setNumeroOrden(request.numeroOrden());
        }

        ActividadCurricularPlanJpaEntity guardada = asignaturaRepository.save(asig);

        recalcularHorasPlan(plan);

        if (sincronizacionBidireccionalPlanService != null) {
            sincronizacionBidireccionalPlanService.propagarActualizacionAsignatura(plan, guardada);
        }

        return toDto(guardada);
    }

    private void validarHoras(Integer horasReloj, Integer horasPresenciales, Integer horasNoPresenciales, Integer horasSemanales) {
        if ((horasReloj != null && horasReloj < 0) ||
            (horasPresenciales != null && horasPresenciales < 0) ||
            (horasNoPresenciales != null && horasNoPresenciales < 0) ||
            (horasSemanales != null && horasSemanales < 0)) {
            throw new DomainValidationException("Las horas deben ser valores numéricos positivos o cero.");
        }
    }

    private void validarAutocorrelatividad(ActividadCurricularPlanJpaEntity asig, ActualizarAsignaturaRequest request) {
        if (request.correlativas() == null || request.correlativas().isBlank()) {
            return;
        }

        String[] tokens = request.correlativas().split("[,;]");
        for (String rawToken : tokens) {
            String token = rawToken.trim();
            if (token.isEmpty()) continue;

            if (token.equalsIgnoreCase(asig.getNombre()) || token.equalsIgnoreCase(request.nombre())) {
                throw new DomainValidationException("Una asignatura no puede ser correlativa de sí misma.");
            }
            if (asig.getCodigo() != null && token.equalsIgnoreCase(asig.getCodigo())) {
                throw new DomainValidationException("Una asignatura no puede ser correlativa de sí misma.");
            }
            if (request.codigo() != null && token.equalsIgnoreCase(request.codigo())) {
                throw new DomainValidationException("Una asignatura no puede ser correlativa de sí misma.");
            }
            if (asig.getId() != null && token.equalsIgnoreCase(asig.getId().toString())) {
                throw new DomainValidationException("Una asignatura no puede ser correlativa de sí misma.");
            }
        }
    }

    private void validarCorrelativasConAsignaturasPrevias(UUID planId, UUID asigId, String correlativas) {
        if (correlativas == null || correlativas.trim().isEmpty() ||
            "Sin correlativas previas".equalsIgnoreCase(correlativas.trim())) {
            return;
        }
        List<ActividadCurricularPlanJpaEntity> existentes =
                asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId);
        boolean existenOtras = existentes != null && existentes.stream()
                .anyMatch(a -> a.getId() != null && !a.getId().equals(asigId));
        if (!existenOtras) {
            throw new DomainValidationException(
                    "No se pueden asignar correlativas si no existen otras asignaturas cargadas previamente en el plan de estudio."
            );
        }
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

    private ActividadCurricularPlanDto toDto(ActividadCurricularPlanJpaEntity a) {
        return new ActividadCurricularPlanDto(
                a.getId(),
                a.getPlanEstudio().getId(),
                a.getNumeroOrden(),
                a.getCodigo(),
                a.getNombre(),
                a.getGrupo(),
                a.getSubgrupo(),
                a.getHorasReloj(),
                a.getHorasPresenciales(),
                a.getHorasNoPresenciales(),
                a.getHorasSemanales(),
                a.getCorrelativas(),
                a.getEsObligatoria(),
                a.getEstadoDictado()
        );
    }
}
