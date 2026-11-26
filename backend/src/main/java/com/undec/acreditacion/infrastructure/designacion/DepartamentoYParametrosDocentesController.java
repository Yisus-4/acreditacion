package com.undec.acreditacion.infrastructure.designacion;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.designacion.dto.*;
import com.undec.acreditacion.infrastructure.designacion.service.DepartamentoYParametrosDocentesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping({"/api/academica", "/api/datos-centrales"})
@PreAuthorize("isAuthenticated()")
public class DepartamentoYParametrosDocentesController {

    private final DepartamentoYParametrosDocentesService service;

    public DepartamentoYParametrosDocentesController(DepartamentoYParametrosDocentesService service) {
        this.service = Objects.requireNonNull(service);
    }

    @GetMapping("/parametros-docentes")
    public ResponseEntity<CatalogosDocentesDto> obtenerCatalogos(
            @RequestParam(defaultValue = "false") boolean incluirInactivos
    ) {
        return ResponseEntity.ok(service.obtenerCatalogos(incluirInactivos));
    }

    // --- Departamentos Académicos ---

    @GetMapping("/departamentos")
    public ResponseEntity<List<DepartamentoAcademicoDto>> listarDepartamentos(
            @RequestParam(defaultValue = "false") boolean incluirInactivos
    ) {
        return ResponseEntity.ok(service.listarDepartamentos(incluirInactivos));
    }

    @PostMapping("/departamentos")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<DepartamentoAcademicoDto> crearDepartamento(
            @RequestBody GuardarDepartamentoAcademicoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearDepartamento(request));
    }

    @PutMapping("/departamentos/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<DepartamentoAcademicoDto> actualizarDepartamento(
            @PathVariable UUID id,
            @RequestBody GuardarDepartamentoAcademicoRequest request
    ) {
        return ResponseEntity.ok(service.actualizarDepartamento(id, request));
    }

    // --- Cargos Docentes ---

    @GetMapping("/parametros-docentes/cargos")
    public ResponseEntity<List<CargoDocenteDto>> listarCargos(
            @RequestParam(defaultValue = "false") boolean incluirInactivos
    ) {
        return ResponseEntity.ok(service.listarCargos(incluirInactivos));
    }

    @PostMapping("/parametros-docentes/cargos")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<CargoDocenteDto> crearCargo(@RequestBody GuardarCargoDocenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCargo(request));
    }

    @PutMapping("/parametros-docentes/cargos/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<CargoDocenteDto> actualizarCargo(
            @PathVariable UUID id,
            @RequestBody GuardarCargoDocenteRequest request
    ) {
        return ResponseEntity.ok(service.actualizarCargo(id, request));
    }

    // --- Dedicaciones Docentes ---

    @GetMapping("/parametros-docentes/dedicaciones")
    public ResponseEntity<List<DedicacionDocenteDto>> listarDedicaciones(
            @RequestParam(defaultValue = "false") boolean incluirInactivos
    ) {
        return ResponseEntity.ok(service.listarDedicaciones(incluirInactivos));
    }

    @PostMapping("/parametros-docentes/dedicaciones")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<DedicacionDocenteDto> crearDedicacion(@RequestBody GuardarDedicacionDocenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearDedicacion(request));
    }

    @PutMapping("/parametros-docentes/dedicaciones/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<DedicacionDocenteDto> actualizarDedicacion(
            @PathVariable UUID id,
            @RequestBody GuardarDedicacionDocenteRequest request
    ) {
        return ResponseEntity.ok(service.actualizarDedicacion(id, request));
    }

    // --- Tipos de Designación ---

    @GetMapping("/parametros-docentes/tipos-designacion")
    public ResponseEntity<List<TipoDesignacionDocenteDto>> listarTiposDesignacion(
            @RequestParam(defaultValue = "false") boolean incluirInactivos
    ) {
        return ResponseEntity.ok(service.listarTiposDesignacion(incluirInactivos));
    }

    @PostMapping("/parametros-docentes/tipos-designacion")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<TipoDesignacionDocenteDto> crearTipoDesignacion(
            @RequestBody GuardarTipoDesignacionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearTipoDesignacion(request));
    }

    @PutMapping("/parametros-docentes/tipos-designacion/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<TipoDesignacionDocenteDto> actualizarTipoDesignacion(
            @PathVariable UUID id,
            @RequestBody GuardarTipoDesignacionRequest request
    ) {
        return ResponseEntity.ok(service.actualizarTipoDesignacion(id, request));
    }

    // --- Períodos Académicos ---

    @GetMapping("/parametros-docentes/periodos")
    public ResponseEntity<List<PeriodoAcademicoDto>> listarPeriodos(
            @RequestParam(defaultValue = "false") boolean incluirInactivos
    ) {
        return ResponseEntity.ok(service.listarPeriodos(incluirInactivos));
    }

    @PostMapping("/parametros-docentes/periodos")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<PeriodoAcademicoDto> crearPeriodo(@RequestBody GuardarPeriodoAcademicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearPeriodo(request));
    }

    @PutMapping("/parametros-docentes/periodos/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'SECRETARIA_ACADEMICA')")
    public ResponseEntity<PeriodoAcademicoDto> actualizarPeriodo(
            @PathVariable UUID id,
            @RequestBody GuardarPeriodoAcademicoRequest request
    ) {
        return ResponseEntity.ok(service.actualizarPeriodo(id, request));
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
