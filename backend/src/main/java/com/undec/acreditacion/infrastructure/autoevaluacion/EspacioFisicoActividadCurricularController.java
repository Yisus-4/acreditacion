package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AsignarActividadEspacioRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.EspacioFisicoActividadCurricularDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/autoevaluacion/espacios")
public class EspacioFisicoActividadCurricularController {

    private final SpringDataEspacioFisicoRepository espacioFisicoRepository;
    private final SpringDataEspacioFisicoActividadCurricularRepository espacioActividadRepository;
    private final SpringDataPlanEstudioFt001Repository planEstudioRepository;
    private final SpringDataActividadCurricularPlanRepository actividadRepository;

    public EspacioFisicoActividadCurricularController(
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataEspacioFisicoActividadCurricularRepository espacioActividadRepository,
            SpringDataPlanEstudioFt001Repository planEstudioRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository) {
        this.espacioFisicoRepository = espacioFisicoRepository;
        this.espacioActividadRepository = espacioActividadRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.actividadRepository = actividadRepository;
    }

    @GetMapping("/{espacioId}/actividades-curriculares")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'INFRAESTRUCTURA', 'BEDELIA', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    public ResponseEntity<List<EspacioFisicoActividadCurricularDto>> listarActividadesPorEspacio(@PathVariable UUID espacioId) {
        if (!espacioFisicoRepository.existsById(espacioId)) {
            throw new ResourceNotFoundException("No se encontró el espacio físico con ID: " + espacioId);
        }

        List<EspacioFisicoActividadCurricularJpaEntity> entities = espacioActividadRepository.findByEspacioFisicoId(espacioId);
        List<EspacioFisicoActividadCurricularDto> dtos = entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{espacioId}/actividades-curriculares")
    @Transactional
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'INFRAESTRUCTURA')")
    public ResponseEntity<List<EspacioFisicoActividadCurricularDto>> reemplazarActividadesPorEspacio(
            @PathVariable UUID espacioId,
            @RequestBody(required = false) List<AsignarActividadEspacioRequest> asignaciones
    ) {
        EspacioFisicoJpaEntity espacio = espacioFisicoRepository.findById(espacioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el espacio físico con ID: " + espacioId));

        espacioActividadRepository.deleteByEspacioFisicoId(espacioId);
        espacioActividadRepository.flush();

        List<EspacioFisicoActividadCurricularDto> resultado = new ArrayList<>();

        if (asignaciones != null) {
            for (AsignarActividadEspacioRequest req : asignaciones) {
                PlanEstudioFt001JpaEntity plan = planEstudioRepository.findById(req.planEstudioId())
                        .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio con ID: " + req.planEstudioId()));

                ActividadCurricularPlanJpaEntity actividad = actividadRepository.findById(req.actividadCurricularId())
                        .orElseThrow(() -> new ResourceNotFoundException("No se encontró la actividad curricular con ID: " + req.actividadCurricularId()));

                EspacioFisicoActividadCurricularJpaEntity nuevaAsig = new EspacioFisicoActividadCurricularJpaEntity(
                        null,
                        espacio,
                        req.carreraId(),
                        plan,
                        actividad,
                        req.comisionesAsignadas() != null ? req.comisionesAsignadas() : 1,
                        req.observacionesUso()
                );

                EspacioFisicoActividadCurricularJpaEntity guardada = espacioActividadRepository.save(nuevaAsig);
                resultado.add(toDto(guardada));
            }
        }

        return ResponseEntity.ok(resultado);
    }

    private EspacioFisicoActividadCurricularDto toDto(EspacioFisicoActividadCurricularJpaEntity entity) {
        String carreraNombre = entity.getPlanEstudio() != null && entity.getPlanEstudio().getCarrera() != null
                ? entity.getPlanEstudio().getCarrera().getNombre() : null;
        String planNombre = entity.getPlanEstudio() != null ? entity.getPlanEstudio().getNombrePlan() : null;
        String actCodigo = entity.getActividadCurricular() != null ? entity.getActividadCurricular().getCodigo() : null;
        String actNombre = entity.getActividadCurricular() != null ? entity.getActividadCurricular().getNombre() : null;

        return new EspacioFisicoActividadCurricularDto(
                entity.getId(),
                entity.getEspacioFisico().getId(),
                entity.getCarreraId(),
                carreraNombre,
                entity.getPlanEstudio().getId(),
                planNombre,
                entity.getActividadCurricular().getId(),
                actCodigo,
                actNombre,
                entity.getComisionesAsignadas(),
                entity.getObservacionesUso(),
                entity.getCreadoEn()
        );
    }
}
