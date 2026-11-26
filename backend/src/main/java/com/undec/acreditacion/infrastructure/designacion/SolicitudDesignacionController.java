package com.undec.acreditacion.infrastructure.designacion;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.designacion.dto.*;
import com.undec.acreditacion.infrastructure.designacion.service.SolicitudDesignacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping({"/api/academica/solicitudes-designacion", "/api/datos-centrales/solicitudes-designacion"})
@PreAuthorize("isAuthenticated()")
public class SolicitudDesignacionController {

    private final SolicitudDesignacionService service;

    public SolicitudDesignacionController(SolicitudDesignacionService service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO', 'CONSULTA_INSTITUCIONAL', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA')")
    public ResponseEntity<List<SolicitudDesignacionResumenDto>> listarSolicitudes(
            @RequestParam(required = false) UUID periodoId,
            @RequestParam(required = false) UUID departamentoId
    ) {
        return ResponseEntity.ok(service.listarSolicitudes(periodoId, departamentoId));
    }

    @GetMapping("/{solicitudId}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO', 'CONSULTA_INSTITUCIONAL', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA')")
    public ResponseEntity<SolicitudDesignacionDetalleDto> obtenerDetalle(@PathVariable UUID solicitudId) {
        return ResponseEntity.ok(service.obtenerDetalleSolicitud(solicitudId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<SolicitudDesignacionDetalleDto> crearSolicitud(
            @RequestBody CrearSolicitudDesignacionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearSolicitud(request));
    }

    @PostMapping("/{solicitudId}/designaciones")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<DesignacionDocenteItemDto> agregarDesignacion(
            @PathVariable UUID solicitudId,
            @RequestBody GuardarDesignacionDocenteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarDesignacion(solicitudId, request));
    }

    @PutMapping("/{solicitudId}/designaciones/{designacionId}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<DesignacionDocenteItemDto> actualizarDesignacion(
            @PathVariable UUID solicitudId,
            @PathVariable UUID designacionId,
            @RequestBody GuardarDesignacionDocenteRequest request
    ) {
        return ResponseEntity.ok(service.actualizarDesignacion(solicitudId, designacionId, request));
    }

    @DeleteMapping("/{solicitudId}/designaciones/{designacionId}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<Void> eliminarDesignacion(
            @PathVariable UUID solicitudId,
            @PathVariable UUID designacionId
    ) {
        service.eliminarDesignacion(solicitudId, designacionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{solicitudId}/designaciones/{designacionId}/distribucion")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<DesignacionDocenteItemDto> actualizarDistribucion(
            @PathVariable UUID solicitudId,
            @PathVariable UUID designacionId,
            @RequestBody GuardarDistribucionHorariaRequest request
    ) {
        return ResponseEntity.ok(service.actualizarDistribucionHoraria(solicitudId, designacionId, request));
    }

    @PostMapping("/{solicitudId}/designaciones/{designacionId}/asignaciones")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<AsignacionCurricularPeriodoDto> agregarAsignacion(
            @PathVariable UUID solicitudId,
            @PathVariable UUID designacionId,
            @RequestBody AgregarAsignacionCurricularRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.agregarAsignacionCurricular(solicitudId, designacionId, request));
    }

    @DeleteMapping("/{solicitudId}/designaciones/{designacionId}/asignaciones/{asignacionId}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA', 'DIRECTOR_DEPARTAMENTO')")
    public ResponseEntity<Void> eliminarAsignacion(
            @PathVariable UUID solicitudId,
            @PathVariable UUID designacionId,
            @PathVariable UUID asignacionId
    ) {
        service.eliminarAsignacionCurricular(solicitudId, designacionId, asignacionId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(DomainValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "status", 422,
                "error", ex.getMessage() != null ? ex.getMessage() : "Error de validación"
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404,
                "error", ex.getMessage() != null ? ex.getMessage() : "Recurso no encontrado"
        ));
    }
}
