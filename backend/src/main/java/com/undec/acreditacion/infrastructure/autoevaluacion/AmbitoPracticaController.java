package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AmbitoPracticaDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AutoevaluacionCarreraAmbitoDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ValidarPrecedenciaAmbitosService;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProcesoAcreditacionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ambitos-practica")
public class AmbitoPracticaController {

    private final SpringDataAmbitoPracticaRepository ambitoRepository;
    private final SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository;
    private final SpringDataInmuebleRepository inmuebleRepository;
    private final SpringDataConvenioInstitucionalRepository convenioRepository;
    private final ValidarPrecedenciaAmbitosService validarPrecedenciaAmbitosService;
    private final SpringDataCarreraRepository carreraRepository;
    private final SpringDataProcesoAcreditacionRepository procesoRepository;
    private final UserRepository userRepository;

    public AmbitoPracticaController(
            SpringDataAmbitoPracticaRepository ambitoRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository,
            SpringDataInmuebleRepository inmuebleRepository,
            SpringDataConvenioInstitucionalRepository convenioRepository,
            ValidarPrecedenciaAmbitosService validarPrecedenciaAmbitosService,
            SpringDataCarreraRepository carreraRepository,
            SpringDataProcesoAcreditacionRepository procesoRepository,
            UserRepository userRepository
    ) {
        this.ambitoRepository = ambitoRepository;
        this.autoevaluacionAmbitoRepository = autoevaluacionAmbitoRepository;
        this.inmuebleRepository = inmuebleRepository;
        this.convenioRepository = convenioRepository;
        this.validarPrecedenciaAmbitosService = validarPrecedenciaAmbitosService;
        this.carreraRepository = carreraRepository;
        this.procesoRepository = procesoRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<AmbitoPracticaDto>> listar(
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "q", required = false) String query
    ) {
        List<AmbitoPracticaJpaEntity> entities;
        if (query != null && !query.isBlank()) {
            entities = ambitoRepository.buscarPorTexto(query.trim());
        } else if (categoria != null && !categoria.isBlank()) {
            entities = ambitoRepository.findByTipoCategoriaAndActivoTrueOrderByNumeroOrdenAsc(categoria.trim().toUpperCase());
        } else {
            entities = ambitoRepository.findByActivoTrueOrderByNumeroOrdenAsc();
        }

        List<AmbitoPracticaDto> dtos = entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmbitoPracticaDto> obtenerPorId(@PathVariable UUID id) {
        AmbitoPracticaJpaEntity entity = ambitoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el ámbito con ID: " + id));
        return ResponseEntity.ok(toDto(entity));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'AEA')")
    public ResponseEntity<AmbitoPracticaDto> crear(@RequestBody AmbitoPracticaDto dto) {
        if (dto.denominacion() == null || dto.denominacion().isBlank()) {
            throw new DomainValidationException("La denominación del ámbito es obligatoria");
        }
        if (dto.tipoCategoria() == null || dto.tipoCategoria().isBlank()) {
            throw new DomainValidationException("La categoría del ámbito (INTERNO, EXTERNO, UNIDAD_DEMOSTRATIVA, CAM) es obligatoria");
        }

        AmbitoPracticaJpaEntity entity = new AmbitoPracticaJpaEntity();
        entity.setId(dto.id() != null ? dto.id() : UUID.randomUUID());
        mapearDatos(entity, dto);
        Instant now = Instant.now();
        entity.setCreadoEn(now);
        entity.setActualizadoEn(now);

        AmbitoPracticaJpaEntity guardado = ambitoRepository.save(entity);
        return ResponseEntity.ok(toDto(guardado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'AEA')")
    public ResponseEntity<AmbitoPracticaDto> actualizar(@PathVariable UUID id, @RequestBody AmbitoPracticaDto dto) {
        AmbitoPracticaJpaEntity entity = ambitoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el ámbito con ID: " + id));

        if (dto.denominacion() == null || dto.denominacion().isBlank()) {
            throw new DomainValidationException("La denominación del ámbito es obligatoria");
        }

        mapearDatos(entity, dto);
        entity.setActualizadoEn(Instant.now());

        AmbitoPracticaJpaEntity actualizado = ambitoRepository.save(entity);
        return ResponseEntity.ok(toDto(actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'AEA')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        AmbitoPracticaJpaEntity entity = ambitoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el ámbito con ID: " + id));
        entity.setActivo(false);
        entity.setActualizadoEn(Instant.now());
        ambitoRepository.save(entity);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Endpoints de Autoevaluación de Carrera (CU-02 a CU-05)
    // =========================================================================

    @GetMapping({"/carrera/{procesoId}", "/autoevaluacion"})
    public ResponseEntity<List<AutoevaluacionCarreraAmbitoDto>> listarAutoevaluacionCarrera(
            @PathVariable(required = false) UUID procesoId,
            @RequestParam(value = "procesoId", required = false) UUID queryProcesoId,
            @RequestParam(value = "carreraId", required = false) UUID carreraId
    ) {
        UUID effectiveProcesoId = procesoId != null ? procesoId : queryProcesoId;
        List<AutoevaluacionCarreraAmbitoJpaEntity> vinculaciones;

        if (effectiveProcesoId != null && carreraId != null) {
            vinculaciones = autoevaluacionAmbitoRepository.findByProcesoIdAndCarreraIdOrderByAmbitoNumeroOrdenAsc(effectiveProcesoId, carreraId);
            if (vinculaciones.isEmpty()) {
                vinculaciones = autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(effectiveProcesoId);
            }
        } else if (effectiveProcesoId != null) {
            vinculaciones = autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(effectiveProcesoId);
        } else if (carreraId != null) {
            vinculaciones = autoevaluacionAmbitoRepository.findByCarreraIdOrderByAmbitoNumeroOrdenAsc(carreraId);
        } else {
            return ResponseEntity.ok(List.of());
        }

        List<AutoevaluacionCarreraAmbitoDto> dtos = vinculaciones.stream()
                .map(this::toAutoevaluacionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping({"/carrera/{procesoId}", "/autoevaluacion"})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'CAREER_DIRECTOR', 'DIRECTOR_CARRERA', 'SCHOOL_DIRECTOR', 'DIRECTOR_ESCUELA', 'SELF_EVALUATION_COMMITTEE_MEMBER', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<AutoevaluacionCarreraAmbitoDto> guardarVinculacionCarrera(
            @PathVariable(required = false) UUID procesoId,
            @RequestParam(value = "carreraId", required = false) UUID queryCarreraId,
            @RequestBody AutoevaluacionCarreraAmbitoDto dto
    ) {
        UUID effectiveProcesoId = procesoId != null ? procesoId : dto.procesoId();
        if (effectiveProcesoId == null) {
            throw new DomainValidationException("El ID del proceso de autoevaluación es obligatorio");
        }
        if (dto.ambitoId() == null) {
            throw new DomainValidationException("El ID del ámbito de práctica es obligatorio");
        }

        UUID effectiveCarreraId = dto.carreraId() != null ? dto.carreraId() : queryCarreraId;

        // Validar Pertenencia a Unidad Académica (CRIT-02)
        validarPertenenciaUnidadAcademica(effectiveCarreraId, effectiveProcesoId);

        AmbitoPracticaJpaEntity ambito = ambitoRepository.findById(dto.ambitoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el ámbito con ID: " + dto.ambitoId()));

        Optional<AutoevaluacionCarreraAmbitoJpaEntity> existenteOpt = Optional.empty();
        if (effectiveCarreraId != null) {
            existenteOpt = autoevaluacionAmbitoRepository.findByProcesoIdAndCarreraIdAndAmbitoId(effectiveProcesoId, effectiveCarreraId, dto.ambitoId());
        }
        if (existenteOpt.isEmpty()) {
            existenteOpt = autoevaluacionAmbitoRepository.findByProcesoIdAndAmbitoId(effectiveProcesoId, dto.ambitoId());
        }

        AutoevaluacionCarreraAmbitoJpaEntity entity = existenteOpt.orElseGet(AutoevaluacionCarreraAmbitoJpaEntity::new);

        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setProcesoId(effectiveProcesoId);
            entity.setAmbito(ambito);
            entity.setCreadoEn(Instant.now());
        }

        entity.setCarreraId(effectiveCarreraId);
        entity.setUtilizadoPorCarrera(dto.utilizadoPorCarrera() != null ? dto.utilizadoPorCarrera() : true);
        entity.setAsignaturasVinculadas(dto.asignaturasVinculadas());
        entity.setResponsablePractica(dto.responsablePractica());
        entity.setHorariosUsoAnioAnterior(dto.horariosUsoAnioAnterior());
        entity.setTipoPracticaEstudiantes(dto.tipoPracticaEstudiantes());
        entity.setObservacionesCarrera(dto.observacionesCarrera());
        entity.setActualizadoEn(Instant.now());

        AutoevaluacionCarreraAmbitoJpaEntity guardado = autoevaluacionAmbitoRepository.save(entity);
        return ResponseEntity.ok(toAutoevaluacionDto(guardado));
    }

    @PostMapping({"/carrera/{procesoId}/completar", "/autoevaluacion/completar"})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'CAREER_DIRECTOR', 'DIRECTOR_CARRERA', 'SCHOOL_DIRECTOR', 'DIRECTOR_ESCUELA', 'SELF_EVALUATION_COMMITTEE_MEMBER', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<Map<String, Object>> completarActividadAmbitos(
            @PathVariable(required = false) UUID procesoId,
            @RequestParam(value = "procesoId", required = false) UUID queryProcesoId,
            @RequestParam(value = "carreraId", required = false) UUID carreraId,
            @RequestParam(value = "actividad", required = false) String actividad
    ) {
        UUID effectiveProcesoId = procesoId != null ? procesoId : queryProcesoId;
        if (effectiveProcesoId == null && carreraId == null) {
            throw new DomainValidationException("El ID del proceso de autoevaluación es obligatorio");
        }

        // 1. Validar pertenencia a UA
        validarPertenenciaUnidadAcademica(carreraId, effectiveProcesoId);

        // 2. Validar precedencia con Dimensión 8.3
        validarPrecedenciaAmbitosService.validar(effectiveProcesoId, carreraId, actividad);

        // 3. Validar barrera de completitud: al menos 1 ámbito vinculado y utilizado por la carrera de la categoría correspondiente
        if (actividad != null && !actividad.isBlank() && !"MAESTRO_RG006".equalsIgnoreCase(actividad.trim())) {
            String targetCategoria = mapearActividadACategoria(actividad.trim());
            if (targetCategoria != null) {
                List<AutoevaluacionCarreraAmbitoJpaEntity> vinculaciones;
                if (effectiveProcesoId != null && carreraId != null) {
                    vinculaciones = autoevaluacionAmbitoRepository.findByProcesoIdAndCarreraIdOrderByAmbitoNumeroOrdenAsc(effectiveProcesoId, carreraId);
                    if (vinculaciones.isEmpty()) {
                        vinculaciones = autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(effectiveProcesoId);
                    }
                } else if (effectiveProcesoId != null) {
                    vinculaciones = autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(effectiveProcesoId);
                } else {
                    vinculaciones = autoevaluacionAmbitoRepository.findByCarreraIdOrderByAmbitoNumeroOrdenAsc(carreraId);
                }

                boolean tieneVinculacionValida = vinculaciones.stream()
                        .anyMatch(v -> v.getAmbito() != null
                                && targetCategoria.equalsIgnoreCase(v.getAmbito().getTipoCategoria())
                                && Boolean.TRUE.equals(v.getUtilizadoPorCarrera()));

                if (!tieneVinculacionValida) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "No es posible completar la actividad: debe existir al menos un ámbito de práctica vinculado a la carrera.");
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "completado", true,
                "procesoId", effectiveProcesoId != null ? effectiveProcesoId : "",
                "actividad", actividad != null ? actividad : "",
                "mensaje", "Actividad validada y completada exitosamente."
        ));
    }

    private void validarPertenenciaUnidadAcademica(UUID carreraId, UUID effectiveProcesoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return;
        }

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMINISTRATOR".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority())
        );
        if (isAdmin) {
            return;
        }

        UUID resolvedCarreraId = carreraId;
        if (resolvedCarreraId == null && effectiveProcesoId != null) {
            resolvedCarreraId = procesoRepository.findById(effectiveProcesoId)
                    .map(ProcesoAcreditacionJpaEntity::getCarreraId)
                    .orElse(null);
        }

        if (resolvedCarreraId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de la carrera es obligatorio");
        }

        CarreraJpaEntity carrera = carreraRepository.findById(resolvedCarreraId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrera no encontrada con ID: " + carreraId));

        String principalEmail = auth.getName();
        User user = userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));

        if (user.getUnidadAcademicaId() == null || carrera.getUnidadAcademica() == null
                || !carrera.getUnidadAcademica().getId().equals(user.getUnidadAcademicaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Acceso denegado: El usuario no pertenece a la Unidad Académica de la carrera especificada.");
        }
    }

    private String mapearActividadACategoria(String actividad) {
        return switch (actividad) {
            case "INTERNOS_FT008" -> "INTERNO";
            case "EXTERNOS_FT009" -> "EXTERNO";
            case "DEMOSTRATIVAS_FT010" -> "UNIDAD_DEMOSTRATIVA";
            case "CAM_FT011" -> "CAM";
            default -> null;
        };
    }

    private void mapearDatos(AmbitoPracticaJpaEntity entity, AmbitoPracticaDto dto) {
        entity.setNumeroOrden(dto.numeroOrden());
        entity.setDenominacion(dto.denominacion().trim());
        entity.setTipoCategoria(dto.tipoCategoria().trim().toUpperCase());
        entity.setTipoAmbito(dto.tipoAmbito());
        entity.setTipoEstablecimiento(dto.tipoEstablecimiento());

        if (dto.inmuebleId() != null) {
            inmuebleRepository.findById(dto.inmuebleId()).ifPresent(entity::setInmueble);
        } else {
            entity.setInmueble(null);
        }
        entity.setInmuebleNombreReferencia(dto.inmuebleNombre());

        if (dto.convenioId() != null) {
            convenioRepository.findById(dto.convenioId()).ifPresent(entity::setConvenio);
        } else {
            entity.setConvenio(null);
        }

        entity.setDescripcion(dto.descripcion());
        entity.setAnioConstruccion(dto.anioConstruccion());
        entity.setSuperficieM2(dto.superficieM2());
        entity.setCapacidadAlumnos(dto.capacidadAlumnos());
        entity.setDisponibilidadHoraria(dto.disponibilidadHoraria());
        entity.setDireccion(dto.direccion());
        entity.setLocalidad(dto.localidad());
        entity.setProvincia(dto.provincia());
        entity.setDepartamento(dto.departamento());
        entity.setCodigoPostal(dto.codigoPostal());
        entity.setTipoDependencia(dto.tipoDependencia());
        entity.setSector(dto.sector());
        entity.setActividadAlumnoCam(dto.actividadAlumnoCam());

        entity.setEspeciesAnimalesCultivos(dto.especiesAnimalesCultivos());
        entity.setNumeroAnimales(dto.numeroAnimales());
        entity.setActividadesDemostrativas(dto.actividadesDemostrativas());
        entity.setCarrerasQueLoUsan(dto.carrerasQueLoUsan());
        if (dto.activo() != null) {
            entity.setActivo(dto.activo());
        }
    }

    private AmbitoPracticaDto toDto(AmbitoPracticaJpaEntity entity) {
        String inmuebleNombre = entity.getInmueble() != null
                ? entity.getInmueble().getNombre()
                : entity.getInmuebleNombreReferencia();
        UUID inmuebleId = entity.getInmueble() != null ? entity.getInmueble().getId() : null;

        String convenioNombre = entity.getConvenio() != null
                ? entity.getConvenio().getContraparte() + " (" + (entity.getConvenio().getResolucionRectoral() != null ? entity.getConvenio().getResolucionRectoral() : "Sin N°") + ")"
                : null;
        UUID convenioId = entity.getConvenio() != null ? entity.getConvenio().getId() : null;

        return new AmbitoPracticaDto(
                entity.getId(),
                entity.getNumeroOrden(),
                entity.getDenominacion(),
                entity.getTipoCategoria(),
                entity.getTipoAmbito(),
                entity.getTipoEstablecimiento(),
                inmuebleId,
                inmuebleNombre,
                convenioId,
                convenioNombre,
                entity.getDescripcion(),
                entity.getAnioConstruccion(),
                entity.getSuperficieM2(),
                entity.getCapacidadAlumnos(),
                entity.getDisponibilidadHoraria(),
                entity.getDireccion(),
                entity.getLocalidad(),
                entity.getProvincia(),
                entity.getDepartamento(),
                entity.getCodigoPostal(),
                entity.getTipoDependencia(),
                entity.getSector(),
                entity.getActividadAlumnoCam(),
                entity.getEspeciesAnimalesCultivos(),
                entity.getNumeroAnimales(),
                entity.getActividadesDemostrativas(),
                entity.getCarrerasQueLoUsan(),
                entity.getActivo(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }

    private AutoevaluacionCarreraAmbitoDto toAutoevaluacionDto(AutoevaluacionCarreraAmbitoJpaEntity entity) {
        AmbitoPracticaDto ambitoDto = entity.getAmbito() != null ? toDto(entity.getAmbito()) : null;
        return new AutoevaluacionCarreraAmbitoDto(
                entity.getId(),
                entity.getProcesoId(),
                entity.getCarreraId(),
                entity.getAmbito() != null ? entity.getAmbito().getId() : null,
                ambitoDto,
                entity.getUtilizadoPorCarrera(),
                entity.getAsignaturasVinculadas(),
                entity.getResponsablePractica(),
                entity.getHorariosUsoAnioAnterior(),
                entity.getTipoPracticaEstudiantes(),
                entity.getObservacionesCarrera(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }
}
