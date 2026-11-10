package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AmbitoPracticaDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.AutoevaluacionCarreraAmbitoDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ValidarPrecedenciaAmbitosService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    public AmbitoPracticaController(
            SpringDataAmbitoPracticaRepository ambitoRepository,
            SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository,
            SpringDataInmuebleRepository inmuebleRepository,
            SpringDataConvenioInstitucionalRepository convenioRepository,
            ValidarPrecedenciaAmbitosService validarPrecedenciaAmbitosService
    ) {
        this.ambitoRepository = ambitoRepository;
        this.autoevaluacionAmbitoRepository = autoevaluacionAmbitoRepository;
        this.inmuebleRepository = inmuebleRepository;
        this.convenioRepository = convenioRepository;
        this.validarPrecedenciaAmbitosService = validarPrecedenciaAmbitosService;
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_MANAGE')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_MANAGE')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_MANAGE')")
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
        if (effectiveProcesoId == null) {
            return ResponseEntity.ok(List.of());
        }
        List<AutoevaluacionCarreraAmbitoJpaEntity> vinculaciones = autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(effectiveProcesoId);
        List<AutoevaluacionCarreraAmbitoDto> dtos = vinculaciones.stream()
                .map(this::toAutoevaluacionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping({"/carrera/{procesoId}", "/autoevaluacion"})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<AutoevaluacionCarreraAmbitoDto> guardarVinculacionCarrera(
            @PathVariable(required = false) UUID procesoId,
            @RequestBody AutoevaluacionCarreraAmbitoDto dto
    ) {
        UUID effectiveProcesoId = procesoId != null ? procesoId : dto.procesoId();
        if (effectiveProcesoId == null) {
            throw new DomainValidationException("El ID del proceso de autoevaluación es obligatorio");
        }
        if (dto.ambitoId() == null) {
            throw new DomainValidationException("El ID del ámbito de práctica es obligatorio");
        }

        AmbitoPracticaJpaEntity ambito = ambitoRepository.findById(dto.ambitoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el ámbito con ID: " + dto.ambitoId()));

        Optional<AutoevaluacionCarreraAmbitoJpaEntity> existenteOpt = autoevaluacionAmbitoRepository.findByProcesoIdAndAmbitoId(effectiveProcesoId, dto.ambitoId());
        AutoevaluacionCarreraAmbitoJpaEntity entity = existenteOpt.orElseGet(AutoevaluacionCarreraAmbitoJpaEntity::new);

        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setProcesoId(effectiveProcesoId);
            entity.setAmbito(ambito);
            entity.setCreadoEn(Instant.now());
        }

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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<Map<String, Object>> completarActividadAmbitos(
            @PathVariable(required = false) UUID procesoId,
            @RequestParam(value = "procesoId", required = false) UUID queryProcesoId,
            @RequestParam(value = "actividad", required = false) String actividad
    ) {
        UUID effectiveProcesoId = procesoId != null ? procesoId : queryProcesoId;
        if (effectiveProcesoId == null) {
            throw new DomainValidationException("El ID del proceso de autoevaluación es obligatorio");
        }

        validarPrecedenciaAmbitosService.validar(effectiveProcesoId, actividad);

        return ResponseEntity.ok(Map.of(
                "completado", true,
                "procesoId", effectiveProcesoId,
                "actividad", actividad != null ? actividad : "",
                "mensaje", "Actividad validada y completada exitosamente."
        ));
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
