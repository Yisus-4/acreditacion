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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final SpringDataAutoevaluacionAmbitoActividadRepository autoevaluacionAmbitoActividadRepository;
    private final SpringDataActividadCurricularPlanRepository actividadCurricularRepository;

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
        this(
                ambitoRepository,
                autoevaluacionAmbitoRepository,
                inmuebleRepository,
                convenioRepository,
                validarPrecedenciaAmbitosService,
                carreraRepository,
                procesoRepository,
                userRepository,
                null,
                null
        );
    }

    @Autowired
    public AmbitoPracticaController(
            SpringDataAmbitoPracticaRepository ambitoRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository,
            SpringDataInmuebleRepository inmuebleRepository,
            SpringDataConvenioInstitucionalRepository convenioRepository,
            ValidarPrecedenciaAmbitosService validarPrecedenciaAmbitosService,
            SpringDataCarreraRepository carreraRepository,
            SpringDataProcesoAcreditacionRepository procesoRepository,
            UserRepository userRepository,
            SpringDataAutoevaluacionAmbitoActividadRepository autoevaluacionAmbitoActividadRepository,
            SpringDataActividadCurricularPlanRepository actividadCurricularRepository
    ) {
        this.ambitoRepository = ambitoRepository;
        this.autoevaluacionAmbitoRepository = autoevaluacionAmbitoRepository;
        this.inmuebleRepository = inmuebleRepository;
        this.convenioRepository = convenioRepository;
        this.validarPrecedenciaAmbitosService = validarPrecedenciaAmbitosService;
        this.carreraRepository = carreraRepository;
        this.procesoRepository = procesoRepository;
        this.userRepository = userRepository;
        this.autoevaluacionAmbitoActividadRepository = autoevaluacionAmbitoActividadRepository;
        this.actividadCurricularRepository = actividadCurricularRepository;
    }

    @GetMapping
    public ResponseEntity<List<AmbitoPracticaDto>> listar(
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "procesoId", required = false) UUID procesoId
    ) {
        List<AmbitoPracticaJpaEntity> entities;
        if (procesoId != null) {
            if (query != null && !query.isBlank()) {
                entities = ambitoRepository.buscarPorProcesoYTexto(procesoId, query.trim());
            } else if (categoria != null && !categoria.isBlank()) {
                entities = ambitoRepository.findByProcesoIdAndTipoCategoriaAndActivoTrueOrderByNumeroOrdenAsc(
                        procesoId, categoria.trim().toUpperCase());
            } else {
                entities = ambitoRepository.findByProcesoIdAndActivoTrueOrderByNumeroOrdenAsc(procesoId);
            }
        } else {
            if (query != null && !query.isBlank()) {
                entities = ambitoRepository.buscarMaestrosPorTexto(query.trim());
            } else if (categoria != null && !categoria.isBlank()) {
                entities = ambitoRepository.findByEsMaestroTrueAndTipoCategoriaAndActivoTrueOrderByNumeroOrdenAsc(
                        categoria.trim().toUpperCase());
            } else {
                entities = ambitoRepository.findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc();
            }
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
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'DIRECTOR_ESCUELA', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<AmbitoPracticaDto> crear(@RequestBody AmbitoPracticaDto dto) {
        if (dto.denominacion() == null || dto.denominacion().isBlank()) {
            throw new DomainValidationException("La denominación del ámbito es obligatoria");
        }
        if (dto.tipoCategoria() == null || dto.tipoCategoria().isBlank()) {
            throw new DomainValidationException("La categoría del ámbito (INTERNO, EXTERNO, UNIDAD_DEMOSTRATIVA, CAM) es obligatoria");
        }

        boolean esCopiaProceso = dto.procesoId() != null && Boolean.FALSE.equals(dto.esMaestro());
        validarPermisoGestionAmbito(!esCopiaProceso, dto.procesoId());

        AmbitoPracticaJpaEntity entity = new AmbitoPracticaJpaEntity();
        entity.setId(dto.id() != null ? dto.id() : UUID.randomUUID());
        entity.setEsMaestro(!esCopiaProceso);
        entity.setProcesoId(esCopiaProceso ? dto.procesoId() : null);
        entity.setOrigenMaestroId(esCopiaProceso ? dto.origenMaestroId() : null);
        mapearDatos(entity, dto);
        Instant now = Instant.now();
        entity.setCreadoEn(now);
        entity.setActualizadoEn(now);

        AmbitoPracticaJpaEntity guardado = ambitoRepository.save(entity);
        return ResponseEntity.ok(toDto(guardado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'DIRECTOR_ESCUELA', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<AmbitoPracticaDto> actualizar(@PathVariable UUID id, @RequestBody AmbitoPracticaDto dto) {
        AmbitoPracticaJpaEntity entity = ambitoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el ámbito con ID: " + id));

        if (dto.denominacion() == null || dto.denominacion().isBlank()) {
            throw new DomainValidationException("La denominación del ámbito es obligatoria");
        }

        boolean esMaestro = Boolean.TRUE.equals(entity.getEsMaestro());
        validarPermisoGestionAmbito(esMaestro, entity.getProcesoId());

        mapearDatos(entity, dto);
        entity.setActualizadoEn(Instant.now());

        AmbitoPracticaJpaEntity actualizado = ambitoRepository.save(entity);
        return ResponseEntity.ok(toDto(actualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'DIRECTOR_ESCUELA', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        AmbitoPracticaJpaEntity entity = ambitoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el ámbito con ID: " + id));

        boolean esMaestro = Boolean.TRUE.equals(entity.getEsMaestro());
        validarPermisoGestionAmbito(esMaestro, entity.getProcesoId());

        entity.setActivo(false);
        entity.setActualizadoEn(Instant.now());
        ambitoRepository.save(entity);

        if (!esMaestro) {
            List<AutoevaluacionCarreraAmbitoJpaEntity> vinculaciones = autoevaluacionAmbitoRepository.findByAmbitoId(entity.getId());
            if (!vinculaciones.isEmpty()) {
                autoevaluacionAmbitoRepository.deleteAll(vinculaciones);
            }
        }
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

        List<AutoevaluacionCarreraAmbitoJpaEntity> filtradas = vinculaciones.stream()
                .filter(v -> esVinculacionValidaParaProceso(v, effectiveProcesoId))
                .filter(v -> carreraId == null || v.getCarreraId() == null || carreraId.equals(v.getCarreraId()))
                .toList();

        Map<UUID, List<AutoevaluacionAmbitoActividadJpaEntity>> actividadesPorVinculo =
                cargarActividadesPorVinculo(filtradas);

        List<AutoevaluacionCarreraAmbitoDto> dtos = filtradas.stream()
                .map(v -> toAutoevaluacionDto(v, actividadesPorVinculo.getOrDefault(v.getId(), List.of())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping({"/carrera/{procesoId}", "/autoevaluacion"})
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'DIRECTOR_ESCUELA', 'COMISION_AUTOEVALUACION')")
    @Transactional
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

        if (Boolean.TRUE.equals(ambito.getEsMaestro())
                || ambito.getProcesoId() == null
                || !effectiveProcesoId.equals(ambito.getProcesoId())) {
            throw new DomainValidationException(
                    "Solo se pueden vincular ámbitos clonados o propios del proceso de autoevaluación."
            );
        }

        Optional<AutoevaluacionCarreraAmbitoJpaEntity> existenteOpt;
        if (effectiveCarreraId != null) {
            existenteOpt = autoevaluacionAmbitoRepository.findByProcesoIdAndCarreraIdAndAmbitoId(
                    effectiveProcesoId, effectiveCarreraId, dto.ambitoId());
            if (existenteOpt.isEmpty()) {
                existenteOpt = autoevaluacionAmbitoRepository.findByProcesoIdAndAmbitoId(effectiveProcesoId, dto.ambitoId())
                        .filter(e -> e.getCarreraId() == null);
            }
        } else {
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
        if (dto.getActividadCurricularIds() != null) {
            List<String> manualesLimpias = limpiarListaStrings(dto.getAsignaturasManuales());
            entity.setAsignaturasVinculadas(manualesLimpias.isEmpty() ? null : String.join("; ", manualesLimpias));
        } else {
            entity.setAsignaturasVinculadas(dto.asignaturasVinculadas());
        }
        entity.setResponsablePractica(dto.responsablePractica());
        entity.setHorariosUsoAnioAnterior(dto.horariosUsoAnioAnterior());
        entity.setTipoPracticaEstudiantes(dto.tipoPracticaEstudiantes());
        entity.setObservacionesCarrera(dto.observacionesCarrera());
        entity.setActualizadoEn(Instant.now());

        AutoevaluacionCarreraAmbitoJpaEntity guardado = autoevaluacionAmbitoRepository.save(entity);

        List<AutoevaluacionAmbitoActividadJpaEntity> linksFinales;
        if (dto.getActividadCurricularIds() != null) {
            linksFinales = sincronizarActividadesVinculadas(guardado, dto.getActividadCurricularIds(), effectiveProcesoId);
        } else if (autoevaluacionAmbitoActividadRepository != null && guardado.getId() != null) {
            List<AutoevaluacionAmbitoActividadJpaEntity> existentes =
                    autoevaluacionAmbitoActividadRepository.findByAutoevaluacionAmbitoId(guardado.getId());
            linksFinales = existentes != null ? existentes : List.of();
        } else {
            linksFinales = List.of();
        }

        return ResponseEntity.ok(toAutoevaluacionDto(guardado, linksFinales));
    }

    @PostMapping({"/carrera/{procesoId}/completar", "/autoevaluacion/completar"})
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_CARRERA', 'DIRECTOR_ESCUELA', 'COMISION_AUTOEVALUACION')")
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
                        .anyMatch(v -> esVinculacionValidaParaProceso(v, effectiveProcesoId)
                                && (carreraId == null || v.getCarreraId() == null || carreraId.equals(v.getCarreraId()))
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

    private void validarPermisoGestionAmbito(boolean esMaestro, UUID procesoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return;
        }

        boolean esAea = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_AEA".equals(a.getAuthority()));

        if (esMaestro) {
            if (!esAea) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Solo usuarios con rol AEA pueden gestionar el Padrón Maestro Institucional (RG-006)."
                );
            }
            return;
        }

        if (!esAea) {
            validarPertenenciaUnidadAcademica(null, procesoId);
        }
    }

    private void validarPertenenciaUnidadAcademica(UUID carreraId, UUID effectiveProcesoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return;
        }

        boolean isAea = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_AEA".equals(a.getAuthority()));
        if (isAea) {
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
                entity.getActualizadoEn(),
                entity.getEsMaestro(),
                entity.getProcesoId(),
                entity.getOrigenMaestroId()
        );
    }

    private boolean esVinculacionValidaParaProceso(AutoevaluacionCarreraAmbitoJpaEntity v, UUID effectiveProcesoId) {
        if (v == null || v.getAmbito() == null) {
            return false;
        }
        AmbitoPracticaJpaEntity ambito = v.getAmbito();
        if (!Boolean.TRUE.equals(ambito.getActivo())) {
            return false;
        }
        if (effectiveProcesoId != null) {
            return !Boolean.TRUE.equals(ambito.getEsMaestro())
                    && effectiveProcesoId.equals(ambito.getProcesoId());
        }
        return true;
    }

    private Map<UUID, List<AutoevaluacionAmbitoActividadJpaEntity>> cargarActividadesPorVinculo(
            List<AutoevaluacionCarreraAmbitoJpaEntity> vinculaciones
    ) {
        if (autoevaluacionAmbitoActividadRepository == null || vinculaciones == null || vinculaciones.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = vinculaciones.stream()
                .map(AutoevaluacionCarreraAmbitoJpaEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<AutoevaluacionAmbitoActividadJpaEntity> links =
                autoevaluacionAmbitoActividadRepository.findByAutoevaluacionAmbitoIdIn(ids);
        if (links == null || links.isEmpty()) {
            return Map.of();
        }
        return links.stream()
                .filter(l -> l.getAutoevaluacionAmbito() != null && l.getAutoevaluacionAmbito().getId() != null)
                .collect(Collectors.groupingBy(l -> l.getAutoevaluacionAmbito().getId()));
    }

    private List<AutoevaluacionAmbitoActividadJpaEntity> sincronizarActividadesVinculadas(
            AutoevaluacionCarreraAmbitoJpaEntity guardado,
            List<UUID> actividadCurricularIds,
            UUID effectiveProcesoId
    ) {
        if (autoevaluacionAmbitoActividadRepository == null || actividadCurricularRepository == null) {
            return List.of();
        }

        Set<UUID> requestedIds = new LinkedHashSet<>();
        for (UUID actId : actividadCurricularIds) {
            if (actId != null) {
                requestedIds.add(actId);
            }
        }

        Map<UUID, ActividadCurricularPlanJpaEntity> actividadesValidas = new LinkedHashMap<>();
        if (!requestedIds.isEmpty()) {
            List<ActividadCurricularPlanJpaEntity> encontradas = actividadCurricularRepository.findAllById(requestedIds);
            if (encontradas != null) {
                Map<UUID, ActividadCurricularPlanJpaEntity> porId = new HashMap<>();
                for (ActividadCurricularPlanJpaEntity act : encontradas) {
                    if (act.getPlanEstudio() != null
                            && act.getPlanEstudio().getProcesoId() != null
                            && !effectiveProcesoId.equals(act.getPlanEstudio().getProcesoId())) {
                        continue;
                    }
                    porId.put(act.getId(), act);
                }
                for (UUID id : requestedIds) {
                    ActividadCurricularPlanJpaEntity act = porId.get(id);
                    if (act != null) {
                        actividadesValidas.put(id, act);
                    }
                }
            }
        }

        List<AutoevaluacionAmbitoActividadJpaEntity> existentes =
                autoevaluacionAmbitoActividadRepository.findByAutoevaluacionAmbitoId(guardado.getId());
        if (existentes == null) {
            existentes = List.of();
        }

        List<AutoevaluacionAmbitoActividadJpaEntity> paraEliminar = new ArrayList<>();
        List<AutoevaluacionAmbitoActividadJpaEntity> conservadas = new ArrayList<>();
        Set<UUID> existentesIds = new HashSet<>();

        for (AutoevaluacionAmbitoActividadJpaEntity ex : existentes) {
            UUID actId = ex.getActividadCurricular() != null ? ex.getActividadCurricular().getId() : null;
            if (actId != null && actividadesValidas.containsKey(actId)) {
                conservadas.add(ex);
                existentesIds.add(actId);
            } else {
                paraEliminar.add(ex);
            }
        }

        if (!paraEliminar.isEmpty()) {
            autoevaluacionAmbitoActividadRepository.deleteAll(paraEliminar);
            autoevaluacionAmbitoActividadRepository.flush();
        }

        List<AutoevaluacionAmbitoActividadJpaEntity> nuevas = new ArrayList<>();
        for (Map.Entry<UUID, ActividadCurricularPlanJpaEntity> entry : actividadesValidas.entrySet()) {
            if (!existentesIds.contains(entry.getKey())) {
                nuevas.add(new AutoevaluacionAmbitoActividadJpaEntity(null, guardado, entry.getValue()));
            }
        }
        if (!nuevas.isEmpty()) {
            List<AutoevaluacionAmbitoActividadJpaEntity> guardadas =
                    autoevaluacionAmbitoActividadRepository.saveAll(nuevas);
            if (guardadas != null && !guardadas.isEmpty()) {
                conservadas.addAll(guardadas);
            } else {
                conservadas.addAll(nuevas);
            }
        }

        return conservadas;
    }

    private List<String> limpiarListaStrings(List<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String item : items) {
            if (item != null) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> parsearTokensManuales(String asignaturasTexto) {
        if (asignaturasTexto == null || asignaturasTexto.isBlank()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String raw : asignaturasTexto.split("[,;]")) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return new ArrayList<>(result);
    }

    private AutoevaluacionCarreraAmbitoDto toAutoevaluacionDto(
            AutoevaluacionCarreraAmbitoJpaEntity entity,
            List<AutoevaluacionAmbitoActividadJpaEntity> actividadesRelacionadas
    ) {
        AmbitoPracticaDto ambitoDto = entity.getAmbito() != null ? toDto(entity.getAmbito()) : null;

        List<ActividadCurricularPlanJpaEntity> actividadesOrdenadas = actividadesRelacionadas == null
                ? List.of()
                : actividadesRelacionadas.stream()
                        .map(AutoevaluacionAmbitoActividadJpaEntity::getActividadCurricular)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(
                                ActividadCurricularPlanJpaEntity::getNumeroOrden,
                                Comparator.nullsLast(Integer::compareTo)
                        ).thenComparing(
                                a -> a.getNombre() != null ? a.getNombre() : "",
                                String.CASE_INSENSITIVE_ORDER
                        ))
                        .toList();

        List<UUID> actividadIds = actividadesOrdenadas.stream()
                .map(ActividadCurricularPlanJpaEntity::getId)
                .filter(Objects::nonNull)
                .toList();

        List<AutoevaluacionCarreraAmbitoDto.ActividadVinculadaResumen> detalle = actividadesOrdenadas.stream()
                .map(a -> new AutoevaluacionCarreraAmbitoDto.ActividadVinculadaResumen(
                        a.getId(), a.getCodigo(), a.getNombre()))
                .toList();

        List<String> manuales = parsearTokensManuales(entity.getAsignaturasVinculadas());

        List<String> labelsDisplay = new ArrayList<>();
        for (AutoevaluacionCarreraAmbitoDto.ActividadVinculadaResumen d : detalle) {
            String nombre = d.nombre() != null ? d.nombre().trim() : "";
            String codigo = d.codigo() != null ? d.codigo().trim() : "";
            if (!codigo.isEmpty()) {
                labelsDisplay.add(nombre + " [" + codigo + "]");
            } else if (!nombre.isEmpty()) {
                labelsDisplay.add(nombre);
            }
        }
        labelsDisplay.addAll(manuales);

        String asignaturasDisplay = labelsDisplay.isEmpty()
                ? entity.getAsignaturasVinculadas()
                : String.join("; ", labelsDisplay);

        AutoevaluacionCarreraAmbitoDto dto = new AutoevaluacionCarreraAmbitoDto(
                entity.getId(),
                entity.getProcesoId(),
                entity.getCarreraId(),
                entity.getAmbito() != null ? entity.getAmbito().getId() : null,
                ambitoDto,
                entity.getUtilizadoPorCarrera(),
                asignaturasDisplay,
                entity.getResponsablePractica(),
                entity.getHorariosUsoAnioAnterior(),
                entity.getTipoPracticaEstudiantes(),
                entity.getObservacionesCarrera(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
        dto.setActividadCurricularIds(actividadIds);
        dto.setActividadesVinculadasDetalle(detalle);
        dto.setAsignaturasManuales(manuales);
        return dto;
    }
}
