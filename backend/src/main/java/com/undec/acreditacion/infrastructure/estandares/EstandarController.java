package com.undec.acreditacion.infrastructure.estandares;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.infrastructure.estandares.dto.CambiarVigenciaEstandarRequest;
import com.undec.acreditacion.infrastructure.estandares.dto.EstandarDetalleDto;
import com.undec.acreditacion.infrastructure.estandares.dto.EstandarResumenDto;
import com.undec.acreditacion.infrastructure.estandares.dto.GuardarEstandarRequest;
import com.undec.acreditacion.infrastructure.estandares.service.EstandarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping({"/api/academica/estandares", "/api/estandares"})
public class EstandarController {

    private final EstandarService estandarService;

    public EstandarController(EstandarService estandarService) {
        this.estandarService = Objects.requireNonNull(estandarService);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'INFRAESTRUCTURA', 'BEDELIA', 'DOCENTE')")
    public ResponseEntity<List<EstandarResumenDto>> listar(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) EstadoVigenciaEstandar estadoVigencia
    ) {
        return ResponseEntity.ok(estandarService.listar(carreraId, estadoVigencia));
    }

    @GetMapping("/vigente")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'INFRAESTRUCTURA', 'BEDELIA', 'DOCENTE')")
    public ResponseEntity<EstandarDetalleDto> obtenerVigentePorCarrera(@RequestParam UUID carreraId) {
        return ResponseEntity.ok(estandarService.obtenerVigentePorCarrera(carreraId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'DIRECTOR_DEPARTAMENTO', 'COMISION_AUTOEVALUACION', 'INFRAESTRUCTURA', 'BEDELIA', 'DOCENTE')")
    public ResponseEntity<EstandarDetalleDto> obtenerDetalle(@PathVariable UUID id) {
        return ResponseEntity.ok(estandarService.obtenerDetalle(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA')")
    public ResponseEntity<EstandarDetalleDto> crear(@RequestBody GuardarEstandarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estandarService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA')")
    public ResponseEntity<EstandarDetalleDto> actualizar(
            @PathVariable UUID id,
            @RequestBody GuardarEstandarRequest request
    ) {
        return ResponseEntity.ok(estandarService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado-vigencia")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA')")
    public ResponseEntity<EstandarDetalleDto> cambiarVigencia(
            @PathVariable UUID id,
            @RequestBody CambiarVigenciaEstandarRequest request
    ) {
        return ResponseEntity.ok(estandarService.cambiarVigencia(id, request));
    }

    @PostMapping("/{id}/nueva-version")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA')")
    public ResponseEntity<EstandarDetalleDto> crearNuevaVersion(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estandarService.crearNuevaVersion(id));
    }
}
