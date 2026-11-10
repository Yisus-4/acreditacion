package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActividadCurricularPlanDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActualizarAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.CrearAsignaturaRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.PlanEstudioFt001Dto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.ActualizarAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.CrearAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.EliminarAsignaturaPlanService;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/planes-estudio")
public class PlanEstudioController {

    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final CrearAsignaturaPlanService crearAsignaturaPlanService;
    private final ActualizarAsignaturaPlanService actualizarAsignaturaPlanService;
    private final EliminarAsignaturaPlanService eliminarAsignaturaPlanService;

    public PlanEstudioController(
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataCarreraRepository carreraRepository,
            CrearAsignaturaPlanService crearAsignaturaPlanService,
            ActualizarAsignaturaPlanService actualizarAsignaturaPlanService,
            EliminarAsignaturaPlanService eliminarAsignaturaPlanService
    ) {
        this.planRepository = planRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.carreraRepository = carreraRepository;
        this.crearAsignaturaPlanService = crearAsignaturaPlanService;
        this.actualizarAsignaturaPlanService = actualizarAsignaturaPlanService;
        this.eliminarAsignaturaPlanService = eliminarAsignaturaPlanService;
    }

    @GetMapping
    public ResponseEntity<List<PlanEstudioFt001Dto>> listar(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) UUID procesoId
    ) {
        if (procesoId != null) {
            List<PlanEstudioFt001JpaEntity> planes = planRepository.findByProcesoIdAndActivoTrue(procesoId);
            List<PlanEstudioFt001Dto> dtos = planes.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        }
        if (carreraId != null) {
            return listarPorCarrera(carreraId);
        }
        List<PlanEstudioFt001JpaEntity> planes = planRepository.findAll();
        List<PlanEstudioFt001Dto> dtos = planes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/carrera/{carreraId}")
    public ResponseEntity<List<PlanEstudioFt001Dto>> listarPorCarrera(@PathVariable UUID carreraId) {
        List<PlanEstudioFt001JpaEntity> planes = planRepository.findByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(carreraId);
        List<PlanEstudioFt001Dto> dtos = planes.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<PlanEstudioFt001Dto> obtenerPorId(@PathVariable UUID id) {
        PlanEstudioFt001JpaEntity plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio con ID: " + id));
        return ResponseEntity.ok(toDto(plan));
    }

    @GetMapping("/{id}/asignaturas")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ActividadCurricularPlanDto>> listarAsignaturas(@PathVariable UUID id) {
        List<ActividadCurricularPlanJpaEntity> asigs = asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(id);
        List<ActividadCurricularPlanDto> dtos = asigs.stream().map(a -> new ActividadCurricularPlanDto(
                a.getId(),
                id,
                a.getNumeroOrden(),
                a.getCodigo(),
                a.getNombre(),
                a.getGrupo(),
                a.getSubgrupo(),
                a.getHorasReloj(),
                a.getHorasPresenciales(),
                a.getHorasNoPresenciales(),
                a.getCorrelativas(),
                a.getEsObligatoria()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<PlanEstudioFt001Dto> crear(@RequestBody PlanEstudioFt001Dto dto) {
        if (dto.carreraId() == null) {
            throw new DomainValidationException("El ID de la carrera es obligatorio");
        }
        CarreraJpaEntity carrera = carreraRepository.findById(dto.carreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera con ID: " + dto.carreraId()));

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(dto.id() != null ? dto.id() : UUID.randomUUID());
        plan.setCarrera(carrera);
        mapearDatosPlan(plan, dto);
        Instant now = Instant.now();
        plan.setCreadoEn(now);
        plan.setActualizadoEn(now);

        if (dto.asignaturas() != null && !dto.asignaturas().isEmpty()) {
            List<ActividadCurricularPlanJpaEntity> listaAsignaturas = new ArrayList<>();
            for (ActividadCurricularPlanDto asigDto : dto.asignaturas()) {
                ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
                asig.setId(asigDto.id() != null ? asigDto.id() : UUID.randomUUID());
                asig.setPlanEstudio(plan);
                asig.setNumeroOrden(asigDto.numeroOrden() != null ? asigDto.numeroOrden() : listaAsignaturas.size() + 1);
                asig.setCodigo(asigDto.codigo());
                asig.setNombre(asigDto.nombre().trim());
                asig.setGrupo(asigDto.grupo());
                asig.setSubgrupo(asigDto.subgrupo());
                asig.setHorasReloj(asigDto.horasReloj());
                asig.setHorasPresenciales(asigDto.horasPresenciales());
                asig.setHorasNoPresenciales(asigDto.horasNoPresenciales());
                asig.setCorrelativas(asigDto.correlativas());
                asig.setEsObligatoria(asigDto.esObligatoria() != null ? asigDto.esObligatoria() : true);
                asig.setCreadoEn(now);
                listaAsignaturas.add(asig);
            }
            plan.setAsignaturas(listaAsignaturas);
        }

        PlanEstudioFt001JpaEntity guardado = planRepository.save(plan);
        return ResponseEntity.ok(toDto(guardado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<PlanEstudioFt001Dto> actualizar(@PathVariable UUID id, @RequestBody PlanEstudioFt001Dto dto) {
        PlanEstudioFt001JpaEntity plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio con ID: " + id));

        mapearDatosPlan(plan, dto);
        plan.setActualizadoEn(Instant.now());

        if (dto.asignaturas() != null) {
            plan.getAsignaturas().clear();
            for (ActividadCurricularPlanDto asigDto : dto.asignaturas()) {
                ActividadCurricularPlanJpaEntity asig = new ActividadCurricularPlanJpaEntity();
                asig.setId(asigDto.id() != null ? asigDto.id() : UUID.randomUUID());
                asig.setPlanEstudio(plan);
                asig.setNumeroOrden(asigDto.numeroOrden() != null ? asigDto.numeroOrden() : plan.getAsignaturas().size() + 1);
                asig.setCodigo(asigDto.codigo());
                asig.setNombre(asigDto.nombre().trim());
                asig.setGrupo(asigDto.grupo());
                asig.setSubgrupo(asigDto.subgrupo());
                asig.setHorasReloj(asigDto.horasReloj());
                asig.setHorasPresenciales(asigDto.horasPresenciales());
                asig.setHorasNoPresenciales(asigDto.horasNoPresenciales());
                asig.setCorrelativas(asigDto.correlativas());
                asig.setEsObligatoria(asigDto.esObligatoria() != null ? asigDto.esObligatoria() : true);
                asig.setCreadoEn(Instant.now());
                plan.getAsignaturas().add(asig);
            }
        }

        PlanEstudioFt001JpaEntity actualizado = planRepository.save(plan);
        return ResponseEntity.ok(toDto(actualizado));
    }

    @PostMapping("/{id}/asignaturas")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<ActividadCurricularPlanDto> crearAsignatura(
            @PathVariable UUID id,
            @RequestBody CrearAsignaturaRequest request
    ) {
        ActividadCurricularPlanDto creada = crearAsignaturaPlanService.ejecutar(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}/asignaturas/{asigId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<ActividadCurricularPlanDto> actualizarAsignatura(
            @PathVariable UUID id,
            @PathVariable UUID asigId,
            @RequestBody ActualizarAsignaturaRequest request
    ) {
        ActividadCurricularPlanDto actualizada = actualizarAsignaturaPlanService.ejecutar(id, asigId, request);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}/asignaturas/{asigId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    public ResponseEntity<Void> eliminarAsignatura(
            @PathVariable UUID id,
            @PathVariable UUID asigId
    ) {
        eliminarAsignaturaPlanService.ejecutar(id, asigId);
        return ResponseEntity.noContent().build();
    }

    private void mapearDatosPlan(PlanEstudioFt001JpaEntity plan, PlanEstudioFt001Dto dto) {
        plan.setCodigoResolucion(dto.codigoResolucion());
        plan.setAnioAprobacion(dto.anioAprobacion());
        plan.setAnioInicio(dto.anioInicio());
        plan.setDuracionAnios(dto.duracionAnios());
        plan.setOrientacionesJson(dto.orientacionesJson());
        plan.setFundamentacionModificaciones(dto.fundamentacionModificaciones());
        plan.setEstructuraSintetica(dto.estructuraSintetica());
        plan.setHorasPresenciales(dto.horasPresenciales());
        plan.setHorasNoPresenciales(dto.horasNoPresenciales());
        plan.setHorasTotales(dto.horasTotales());
        plan.setHorasOptativasMinimas(dto.horasOptativasMinimas());
        plan.setActividadesComunesCiclo(dto.actividadesComunesCiclo());
        plan.setTituloIntermedioDenominacion(dto.tituloIntermedioDenominacion());
        plan.setTituloIntermedioDuracion(dto.tituloIntermedioDuracion());
        plan.setTituloIntermedioActividades(dto.tituloIntermedioActividades());
        plan.setInstanciaFinalGraduacion(dto.instanciaFinalGraduacion());
        plan.setOtraInformacion(dto.otraInformacion());
        if (dto.activo() != null) {
            plan.setActivo(dto.activo());
        }
        if (dto.procesoId() != null) {
            plan.setProcesoId(dto.procesoId());
        }
    }

    private PlanEstudioFt001Dto toDto(PlanEstudioFt001JpaEntity plan) {
        List<ActividadCurricularPlanDto> asigDtos = plan.getAsignaturas() != null
                ? plan.getAsignaturas().stream().map(a -> new ActividadCurricularPlanDto(
                        a.getId(),
                        plan.getId(),
                        a.getNumeroOrden(),
                        a.getCodigo(),
                        a.getNombre(),
                        a.getGrupo(),
                        a.getSubgrupo(),
                        a.getHorasReloj(),
                        a.getHorasPresenciales(),
                        a.getHorasNoPresenciales(),
                        a.getCorrelativas(),
                        a.getEsObligatoria()
                )).collect(Collectors.toList())
                : List.of();

        return new PlanEstudioFt001Dto(
                plan.getId(),
                plan.getCarrera() != null ? plan.getCarrera().getId() : null,
                plan.getCarrera() != null ? plan.getCarrera().getNombre() : null,
                plan.getProcesoId(),
                plan.getCodigoResolucion(),
                plan.getAnioAprobacion(),
                plan.getAnioInicio(),
                plan.getDuracionAnios(),
                plan.getOrientacionesJson(),
                plan.getFundamentacionModificaciones(),
                plan.getEstructuraSintetica(),
                plan.getHorasPresenciales(),
                plan.getHorasNoPresenciales(),
                plan.getHorasTotales(),
                plan.getHorasOptativasMinimas(),
                plan.getActividadesComunesCiclo(),
                plan.getTituloIntermedioDenominacion(),
                plan.getTituloIntermedioDuracion(),
                plan.getTituloIntermedioActividades(),
                plan.getInstanciaFinalGraduacion(),
                plan.getOtraInformacion(),
                plan.getActivo(),
                asigDtos,
                plan.getCreadoEn(),
                plan.getActualizadoEn()
        );
    }
}
