package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ValidarPrecedenciaAmbitosService {

    private static final Set<String> ACTIVIDADES_CON_PRECEDENCIA_8_3 = Set.of(
            "INTERNOS_FT008",
            "EXTERNOS_FT009",
            "DEMOSTRATIVAS_FT010",
            "CAM_FT011"
    );

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;

    public ValidarPrecedenciaAmbitosService(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository
    ) {
        this.planRepository = planRepository;
        this.asignaturaRepository = asignaturaRepository;
    }

    @Transactional(readOnly = true)
    public void validarPrecedenciaParaCompletar(UUID procesoId, String actividad) {
        if (procesoId == null) {
            return;
        }

        if (actividad != null && !ACTIVIDADES_CON_PRECEDENCIA_8_3.contains(actividad.toUpperCase())) {
            return;
        }

        if (!puedeCompletar(procesoId)) {
            throw new DomainValidationException(
                    "Precedencia requerida (CONEAU PRG-002): No es posible completar esta actividad hasta que se carguen las asignaturas en la Dimensión 8.3 (Plan de Estudio)."
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean puedeCompletar(UUID procesoId) {
        if (procesoId == null) {
            return false;
        }
        List<PlanEstudioFt001JpaEntity> planes = planRepository.findByProcesoIdAndActivoTrue(procesoId);
        if (planes.isEmpty()) {
            return false;
        }
        return planes.stream()
                .anyMatch(p -> !asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(p.getId()).isEmpty());
    }
}
