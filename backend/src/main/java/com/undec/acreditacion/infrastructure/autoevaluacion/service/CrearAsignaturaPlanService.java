package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActividadCurricularPlanDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.CrearAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CrearAsignaturaPlanService {

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;

    public CrearAsignaturaPlanService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository
    ) {
        this.planRepository = planRepository;
        this.asignaturaRepository = asignaturaRepository;
    }

    @Transactional
    public ActividadCurricularPlanDto ejecutar(UUID planId, CrearAsignaturaRequest request) {
        return crearAsignatura(planId, request);
    }

    @Transactional
    public ActividadCurricularPlanDto crearAsignatura(UUID planId, CrearAsignaturaRequest request) {
        PlanEstudioFt001JpaEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio con ID: " + planId));

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

        validarHoras(request.horasReloj(), request.horasPresenciales(), request.horasNoPresenciales());
        validarCorrelativasConAsignaturasPrevias(planId, request.correlativas());

        if (request.codigo() != null && !request.codigo().trim().isEmpty()) {
            String codigo = request.codigo().trim();
            if (asignaturaRepository.existsByPlanEstudioIdAndCodigo(planId, codigo)) {
                throw new DomainValidationException("Ya existe una asignatura con el código " + codigo + " en este plan de estudio.");
            }
        }

        int orden = request.numeroOrden() != null ? request.numeroOrden() :
                asignaturaRepository.findMaxNumeroOrdenByPlanEstudioId(planId) + 1;

        ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
        asig.setId(UUID.randomUUID());
        asig.setPlanEstudio(plan);
        asig.setNumeroOrden(orden);
        asig.setCodigo(request.codigo() != null ? request.codigo().trim() : null);
        asig.setNombre(request.nombre().trim());
        asig.setGrupo(request.grupo());
        asig.setSubgrupo(request.subgrupo());
        asig.setHorasReloj(request.horasReloj());
        asig.setHorasPresenciales(request.horasPresenciales());
        asig.setHorasNoPresenciales(request.horasNoPresenciales());
        asig.setCorrelativas(request.correlativas() != null && !request.correlativas().trim().isEmpty() ?
                request.correlativas().trim() : "Sin correlativas previas");
        asig.setEsObligatoria(request.esObligatoria() != null ? request.esObligatoria() : true);
        asig.setCreadoEn(Instant.now());

        ActividadCurricularPlanJpaEntity guardada = asignaturaRepository.save(asig);

        recalcularHorasPlan(plan);

        return toDto(guardada);
    }

    private void validarHoras(Integer horasReloj, Integer horasPresenciales, Integer horasNoPresenciales) {
        if ((horasReloj != null && horasReloj < 0) ||
            (horasPresenciales != null && horasPresenciales < 0) ||
            (horasNoPresenciales != null && horasNoPresenciales < 0)) {
            throw new DomainValidationException("Las horas deben ser valores numéricos positivos o cero.");
        }
    }

    private void validarCorrelativasConAsignaturasPrevias(UUID planId, String correlativas) {
        if (correlativas == null || correlativas.trim().isEmpty() ||
            "Sin correlativas previas".equalsIgnoreCase(correlativas.trim())) {
            return;
        }
        List<ActividadCurricularPlanJpaEntity> existentes =
                asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(planId);
        if (existentes == null || existentes.isEmpty()) {
            throw new DomainValidationException(
                    "No se pueden asignar correlativas si no existen asignaturas cargadas previamente en el plan de estudio."
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
                a.getCorrelativas(),
                a.getEsObligatoria()
        );
    }
}
