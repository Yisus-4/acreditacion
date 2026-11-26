package com.undec.acreditacion.infrastructure.datoscentrales;

import com.undec.acreditacion.domain.entities.EstadoVigenciaPlan;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.*;
import com.undec.acreditacion.infrastructure.datoscentrales.service.DatosCentralesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping({"/api/datos-centrales", "/api/academica"})
@PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'CONSULTA_INSTITUCIONAL', 'INFRAESTRUCTURA', 'BEDELIA')")
public class DatosCentralesController {

    private final DatosCentralesService datosCentralesService;

    public DatosCentralesController(DatosCentralesService datosCentralesService) {
        this.datosCentralesService = Objects.requireNonNull(datosCentralesService);
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenDatosCentralesDto> obtenerResumen() {
        return ResponseEntity.ok(datosCentralesService.obtenerResumen());
    }

    @GetMapping("/planes-estudio")
    public ResponseEntity<List<PlanEstudioMaestroDto>> listarPlanesEstudio(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) EstadoVigenciaPlan estadoVigencia
    ) {
        return ResponseEntity.ok(datosCentralesService.listarPlanesEstudio(carreraId, estadoVigencia));
    }

    @PostMapping("/planes-estudio")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<PlanEstudioMaestroDto> crearPlanEstudio(
            @RequestBody CrearPlanEstudioMaestroRequest request
    ) {
        PlanEstudioMaestroDto creado = datosCentralesService.crearPlanEstudioMaestro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/planes-estudio/{id}/estado-vigencia")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<PlanEstudioMaestroDto> cambiarEstadoVigencia(
            @PathVariable UUID id,
            @RequestBody CambiarVigenciaPlanRequest request
    ) {
        PlanEstudioMaestroDto actualizado = datosCentralesService.cambiarEstadoVigenciaPlan(id, request.estadoVigencia());
        return ResponseEntity.ok(actualizado);
    }

    @PutMapping("/planes-estudio/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<PlanEstudioMaestroDto> actualizarPlanEstudio(
            @PathVariable UUID id,
            @RequestBody CrearPlanEstudioMaestroRequest request
    ) {
        return ResponseEntity.ok(datosCentralesService.actualizarPlanEstudioMaestro(id, request));
    }

    @GetMapping("/cuerpo-academico")
    public ResponseEntity<List<CuerpoAcademicoMaestroDto>> listarCuerpoAcademico(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) UUID departamentoAcademicoId,
            @RequestParam(required = false, defaultValue = "false") boolean incluirInactivos,
            @RequestParam(required = false) String query
    ) {
        if (departamentoAcademicoId == null && !incluirInactivos) {
            return ResponseEntity.ok(datosCentralesService.listarCuerpoAcademico(carreraId, query));
        }
        return ResponseEntity.ok(datosCentralesService.listarCuerpoAcademico(
                carreraId, departamentoAcademicoId, incluirInactivos, query));
    }

    @PostMapping("/cuerpo-academico")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> crearCuerpoAcademico(
            @RequestBody CrearCuerpoAcademicoMaestroRequest request
    ) {
        CuerpoAcademicoMaestroDto creado = datosCentralesService.crearCuerpoAcademicoMaestro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/cuerpo-academico/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> actualizarCuerpoAcademico(
            @PathVariable UUID id,
            @RequestBody CrearCuerpoAcademicoMaestroRequest request
    ) {
        return ResponseEntity.ok(datosCentralesService.actualizarCuerpoAcademicoMaestro(id, request));
    }

    @PostMapping("/cuerpo-academico/{id}/asignaciones")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> agregarAsignacion(
            @PathVariable UUID id,
            @RequestBody CrearCuerpoAcademicoMaestroRequest.CrearAsignacionCatedraItem item
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(datosCentralesService.agregarAsignacionDocente(id, item));
    }

    @DeleteMapping("/cuerpo-academico/{id}/asignaciones/{asignacionId}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> eliminarAsignacion(
            @PathVariable UUID id,
            @PathVariable UUID asignacionId
    ) {
        return ResponseEntity.ok(datosCentralesService.eliminarAsignacionDocente(id, asignacionId));
    }
}
