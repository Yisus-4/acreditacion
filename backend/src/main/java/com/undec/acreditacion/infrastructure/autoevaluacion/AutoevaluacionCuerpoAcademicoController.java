package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.AsignacionCatedraDto;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.CrearCuerpoAcademicoMaestroRequest;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.CuerpoAcademicoMaestroDto;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProcesoAcreditacionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/autoevaluacion/procesos/{procesoId}/cuerpo-academico")
@Transactional
public class AutoevaluacionCuerpoAcademicoController {

    private final SpringDataProcesoCuerpoAcademicoRepository procesoDocenteRepository;
    private final SpringDataProcesoCuerpoAcademicoActividadRepository procesoDocenteActividadRepository;
    private final SpringDataPlanEstudioFt001Repository planEstudioRepository;
    private final SpringDataActividadCurricularPlanRepository actividadRepository;
    private final SpringDataProcesoAcreditacionRepository procesoRepository;
    private final SpringDataCarreraRepository carreraRepository;

    public AutoevaluacionCuerpoAcademicoController(
            SpringDataProcesoCuerpoAcademicoRepository procesoDocenteRepository,
            SpringDataProcesoCuerpoAcademicoActividadRepository procesoDocenteActividadRepository,
            SpringDataPlanEstudioFt001Repository planEstudioRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            SpringDataProcesoAcreditacionRepository procesoRepository,
            SpringDataCarreraRepository carreraRepository
    ) {
        this.procesoDocenteRepository = Objects.requireNonNull(procesoDocenteRepository);
        this.procesoDocenteActividadRepository = Objects.requireNonNull(procesoDocenteActividadRepository);
        this.planEstudioRepository = Objects.requireNonNull(planEstudioRepository);
        this.actividadRepository = Objects.requireNonNull(actividadRepository);
        this.procesoRepository = Objects.requireNonNull(procesoRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CuerpoAcademicoMaestroDto>> listar(
            @PathVariable UUID procesoId,
            @RequestParam(required = false) String query
    ) {
        List<ProcesoCuerpoAcademicoJpaEntity> docs = procesoDocenteRepository.findByProcesoIdAndActivoTrue(procesoId);
        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();
            docs = docs.stream()
                    .filter(d -> containsIgnoreCase(d.getDni(), q)
                            || containsIgnoreCase(d.getApellido(), q)
                            || containsIgnoreCase(d.getNombre(), q))
                    .toList();
        }
        List<CuerpoAcademicoMaestroDto> result = docs.stream()
                .sorted(Comparator.comparing(ProcesoCuerpoAcademicoJpaEntity::getApellido, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ProcesoCuerpoAcademicoJpaEntity::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(d -> toDto(procesoId, d))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> crear(
            @PathVariable UUID procesoId,
            @RequestBody CrearCuerpoAcademicoMaestroRequest request
    ) {
        validarRequestDocente(request);
        String dni = request.dni().trim();
        Optional<ProcesoCuerpoAcademicoJpaEntity> existente = procesoDocenteRepository.findByProcesoIdAndDni(procesoId, dni);
        if (existente.isPresent() && Boolean.TRUE.equals(existente.get().getActivo())) {
            throw new DomainValidationException("Ya existe un docente activo con DNI " + dni + " en esta autoevaluación");
        }

        ProcesoCuerpoAcademicoJpaEntity docente = existente.orElseGet(ProcesoCuerpoAcademicoJpaEntity::new);
        Instant now = Instant.now();
        docente.setProcesoId(procesoId);
        if (docente.getId() == null) {
            docente.setOrigenMaestroId(null);
            docente.setCreadoEn(now);
        }
        docente.setActivo(true);
        docente.setActualizadoEn(now);
        aplicarDatosDocente(docente, request);

        ProcesoCuerpoAcademicoJpaEntity guardado = procesoDocenteRepository.save(docente);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(procesoId, guardado));
    }

    @PutMapping("/{docenteId}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> actualizar(
            @PathVariable UUID procesoId,
            @PathVariable UUID docenteId,
            @RequestBody CrearCuerpoAcademicoMaestroRequest request
    ) {
        validarRequestDocente(request);
        ProcesoCuerpoAcademicoJpaEntity docente = buscarDocenteEnProceso(procesoId, docenteId);

        String nuevoDni = request.dni().trim();
        if (!nuevoDni.equals(docente.getDni())) {
            Optional<ProcesoCuerpoAcademicoJpaEntity> duplicado = procesoDocenteRepository.findByProcesoIdAndDni(procesoId, nuevoDni);
            if (duplicado.isPresent() && !duplicado.get().getId().equals(docenteId) && Boolean.TRUE.equals(duplicado.get().getActivo())) {
                throw new DomainValidationException("Ya existe otro docente con DNI " + nuevoDni + " en esta autoevaluación");
            }
        }

        aplicarDatosDocente(docente, request);
        docente.setActualizadoEn(Instant.now());
        ProcesoCuerpoAcademicoJpaEntity guardado = procesoDocenteRepository.save(docente);
        return ResponseEntity.ok(toDto(procesoId, guardado));
    }

    @DeleteMapping("/{docenteId}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID procesoId,
            @PathVariable UUID docenteId
    ) {
        ProcesoCuerpoAcademicoJpaEntity docente = buscarDocenteEnProceso(procesoId, docenteId);
        docente.setActivo(false);
        docente.setActualizadoEn(Instant.now());
        procesoDocenteRepository.save(docente);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{docenteId}/asignaciones")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> agregarAsignacion(
            @PathVariable UUID procesoId,
            @PathVariable UUID docenteId,
            @RequestBody CrearCuerpoAcademicoMaestroRequest.CrearAsignacionCatedraItem item
    ) {
        if (item == null || item.planEstudioId() == null || item.actividadCurricularId() == null) {
            throw new DomainValidationException("El plan de estudio y la actividad curricular son obligatorios");
        }
        ProcesoCuerpoAcademicoJpaEntity docente = buscarDocenteEnProceso(procesoId, docenteId);

        PlanEstudioFt001JpaEntity plan = planEstudioRepository.findById(item.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + item.planEstudioId()));
        ActividadCurricularPlanJpaEntity actividad = actividadRepository.findById(item.actividadCurricularId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la actividad curricular ID: " + item.actividadCurricularId()));

        UUID carreraId = item.carreraId();
        if (carreraId == null && plan.getCarrera() != null) {
            carreraId = plan.getCarrera().getId();
        }
        if (carreraId == null) {
            carreraId = procesoRepository.findById(procesoId)
                    .map(ProcesoAcreditacionJpaEntity::getCarreraId)
                    .orElseThrow(() -> new DomainValidationException("No se pudo determinar la carrera de la asignación"));
        }

        Optional<ProcesoCuerpoAcademicoActividadJpaEntity> yaExiste = procesoDocenteActividadRepository
                .findByProcesoIdAndDocenteIdAndPlanEstudioIdAndActividadCurricularId(
                        procesoId, docente.getId(), plan.getId(), actividad.getId());

        if (yaExiste.isEmpty()) {
            ProcesoCuerpoAcademicoActividadJpaEntity asig = new ProcesoCuerpoAcademicoActividadJpaEntity();
            asig.setProcesoId(procesoId);
            asig.setDocente(docente);
            asig.setCarreraId(carreraId);
            asig.setPlanEstudio(plan);
            asig.setActividadCurricular(actividad);
            asig.setRolEnCatedra(item.rolEnCatedra() != null && !item.rolEnCatedra().isBlank()
                    ? item.rolEnCatedra().trim()
                    : "PROFESOR_A_CARGO");
            asig.setCargaHorariaSemanal(item.cargaHorariaSemanal() != null ? item.cargaHorariaSemanal() : 4);
            asig.setCreadoEn(Instant.now());
            procesoDocenteActividadRepository.save(asig);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(procesoId, docente));
    }

    @DeleteMapping("/{docenteId}/asignaciones/{asignacionId}")
    @PreAuthorize("hasAnyRole('AEA', 'DIRECTOR_ESCUELA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION')")
    public ResponseEntity<CuerpoAcademicoMaestroDto> eliminarAsignacion(
            @PathVariable UUID procesoId,
            @PathVariable UUID docenteId,
            @PathVariable UUID asignacionId
    ) {
        ProcesoCuerpoAcademicoJpaEntity docente = buscarDocenteEnProceso(procesoId, docenteId);
        ProcesoCuerpoAcademicoActividadJpaEntity asig = procesoDocenteActividadRepository.findById(asignacionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignación ID: " + asignacionId));
        if (!procesoId.equals(asig.getProcesoId()) || asig.getDocente() == null || !docente.getId().equals(asig.getDocente().getId())) {
            throw new DomainValidationException("La asignación no pertenece al docente o proceso indicado");
        }
        procesoDocenteActividadRepository.delete(asig);
        return ResponseEntity.ok(toDto(procesoId, docente));
    }

    private ProcesoCuerpoAcademicoJpaEntity buscarDocenteEnProceso(UUID procesoId, UUID docenteId) {
        ProcesoCuerpoAcademicoJpaEntity docente = procesoDocenteRepository.findById(docenteId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el docente ID: " + docenteId));
        if (!procesoId.equals(docente.getProcesoId())) {
            throw new DomainValidationException("El docente no pertenece a este proceso de autoevaluación");
        }
        return docente;
    }

    private void validarRequestDocente(CrearCuerpoAcademicoMaestroRequest req) {
        if (req == null) throw new DomainValidationException("La solicitud es requerida");
        if (req.dni() == null || req.dni().isBlank()) throw new DomainValidationException("El DNI es obligatorio");
        if (req.apellido() == null || req.apellido().isBlank()) throw new DomainValidationException("El apellido es obligatorio");
        if (req.nombre() == null || req.nombre().isBlank()) throw new DomainValidationException("El nombre es obligatorio");
        if (req.categoriaDocente() == null || req.categoriaDocente().isBlank()) throw new DomainValidationException("La categoría docente es obligatoria");
        if (req.caracterDesignacion() == null || req.caracterDesignacion().isBlank()) throw new DomainValidationException("El carácter de designación es obligatorio");
        if (req.dedicacionDocente() == null || req.dedicacionDocente().isBlank()) throw new DomainValidationException("La dedicación docente es obligatoria");
    }

    private void aplicarDatosDocente(ProcesoCuerpoAcademicoJpaEntity docente, CrearCuerpoAcademicoMaestroRequest req) {
        docente.setDni(req.dni().trim());
        docente.setCuit(req.cuit());
        docente.setApellido(req.apellido().trim());
        docente.setNombre(req.nombre().trim());
        docente.setEmailInstitucional(req.emailInstitucional());
        docente.setEmailPersonal(req.emailPersonal());
        docente.setTelefono(req.telefono());
        docente.setTituloGrado(req.tituloGrado());
        docente.setMaximoTituloPosgrado(req.maximoTituloPosgrado());
        docente.setCategoriaDocente(req.categoriaDocente().trim());
        docente.setCaracterDesignacion(req.caracterDesignacion().trim());
        docente.setDedicacionDocente(req.dedicacionDocente().trim());
        docente.setHorasSemanales(req.horasSemanales() != null ? req.horasSemanales() : 10);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private CuerpoAcademicoMaestroDto toDto(UUID procesoId, ProcesoCuerpoAcademicoJpaEntity d) {
        List<ProcesoCuerpoAcademicoActividadJpaEntity> asigEntities =
                procesoDocenteActividadRepository.findByProcesoIdAndDocenteId(procesoId, d.getId());

        List<AsignacionCatedraDto> asigDtos = asigEntities.stream().map(a -> {
            String carreraNombre = a.getCarreraId() != null
                    ? carreraRepository.findById(a.getCarreraId()).map(CarreraJpaEntity::getNombre).orElse(null)
                    : null;
            PlanEstudioFt001JpaEntity p = a.getPlanEstudio();
            String planNombre = p != null
                    ? (p.getNombrePlan() != null && !p.getNombrePlan().isBlank() ? p.getNombrePlan() : p.getCodigoResolucion())
                    : null;
            return new AsignacionCatedraDto(
                    a.getId(),
                    a.getCarreraId(),
                    carreraNombre,
                    p != null ? p.getId() : null,
                    planNombre,
                    a.getActividadCurricular() != null ? a.getActividadCurricular().getId() : null,
                    a.getActividadCurricular() != null ? a.getActividadCurricular().getNombre() : null,
                    a.getRolEnCatedra(),
                    a.getCargaHorariaSemanal()
            );
        }).toList();

        return new CuerpoAcademicoMaestroDto(
                d.getId(),
                d.getDni(),
                d.getCuit(),
                d.getApellido(),
                d.getNombre(),
                d.getEmailInstitucional(),
                d.getEmailPersonal(),
                d.getTelefono(),
                d.getTituloGrado(),
                d.getMaximoTituloPosgrado(),
                d.getCategoriaDocente(),
                d.getCaracterDesignacion(),
                d.getDedicacionDocente(),
                d.getHorasSemanales(),
                d.getActivo(),
                asigDtos,
                d.getCreadoEn(),
                d.getActualizadoEn()
        );
    }
}
