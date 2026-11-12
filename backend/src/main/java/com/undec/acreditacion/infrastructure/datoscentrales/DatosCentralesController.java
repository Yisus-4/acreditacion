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
@RequestMapping("/api/datos-centrales")
@PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR', 'AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'INFRAESTRUCTURA', 'BEDELIA')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR', 'AEA')")
    public ResponseEntity<PlanEstudioMaestroDto> crearPlanEstudio(
            @RequestBody CrearPlanEstudioMaestroRequest request
    ) {
        PlanEstudioMaestroDto creado = datosCentralesService.crearPlanEstudioMaestro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/planes-estudio/{id}/estado-vigencia")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR', 'AEA')")
    public ResponseEntity<PlanEstudioMaestroDto> cambiarEstadoVigencia(
            @PathVariable UUID id,
            @RequestBody CambiarVigenciaPlanRequest request
    ) {
        PlanEstudioMaestroDto actualizado = datosCentralesService.cambiarEstadoVigenciaPlan(id, request.estadoVigencia());
        return ResponseEntity.ok(actualizado);
    }

    @GetMapping("/cuerpo-academico")
    public ResponseEntity<List<CuerpoAcademicoMaestroDto>> listarCuerpoAcademico(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(datosCentralesService.listarCuerpoAcademico(carreraId, query));
    }

    @PostMapping("/cuerpo-academico")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMINISTRATOR', 'AEA')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> crearCuerpoAcademico(
            @RequestBody CrearCuerpoAcademicoMaestroRequest request
    ) {
        CuerpoAcademicoMaestroDto creado = datosCentralesService.crearCuerpoAcademicoMaestro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
