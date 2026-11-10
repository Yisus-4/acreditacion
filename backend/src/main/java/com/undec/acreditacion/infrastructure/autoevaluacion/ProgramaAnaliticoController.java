package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ProgramaAnaliticoAsignaturaDto;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.ProcesoAcreditacionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataProcesoAcreditacionRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/programas-analiticos")
public class ProgramaAnaliticoController {

    private final SpringDataProgramaAnaliticoAsignaturaRepository programaRepository;
    private final SpringDataPlanEstudioFt001Repository planEstudioRepository;
    private final SpringDataActividadCurricularPlanRepository asignaturaRepository;
    private final SpringDataProcesoAcreditacionRepository procesoRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final DocumentStoragePort storagePort;
    private final UserRepository userRepository;

    public ProgramaAnaliticoController(
            SpringDataProgramaAnaliticoAsignaturaRepository programaRepository,
            SpringDataPlanEstudioFt001Repository planEstudioRepository,
            SpringDataActividadCurricularPlanRepository asignaturaRepository,
            SpringDataProcesoAcreditacionRepository procesoRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            DocumentStoragePort storagePort,
            UserRepository userRepository
    ) {
        this.programaRepository = programaRepository;
        this.planEstudioRepository = planEstudioRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.procesoRepository = procesoRepository;
        this.planTrabajoRepository = planTrabajoRepository;
        this.storagePort = storagePort;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProgramaAnaliticoAsignaturaDto>> listar(
            @RequestParam(required = false) UUID procesoId,
            @RequestParam(required = false) UUID planId
    ) {
        List<ProgramaAnaliticoAsignaturaJpaEntity> list;
        if (planId != null) {
            list = programaRepository.findByPlanEstudioId(planId);
            if (list.isEmpty() && procesoId != null) {
                list = programaRepository.findByProcesoIdOrderByNombreAsignaturaAsc(procesoId);
            }
        } else if (procesoId != null) {
            list = programaRepository.findByProcesoIdOrderByNombreAsignaturaAsc(procesoId);
        } else {
            list = programaRepository.findAll();
        }
        return ResponseEntity.ok(list.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/proceso/{procesoId}")
    public ResponseEntity<List<ProgramaAnaliticoAsignaturaDto>> listarPorProceso(@PathVariable UUID procesoId) {
        List<ProgramaAnaliticoAsignaturaJpaEntity> list = programaRepository.findByProcesoIdOrderByNombreAsignaturaAsc(procesoId);
        List<ProgramaAnaliticoAsignaturaDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/proceso/{procesoId}/inicializar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<List<ProgramaAnaliticoAsignaturaDto>> inicializarDesdePlan(@PathVariable UUID procesoId) {
        ProcesoAcreditacionJpaEntity proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación: " + procesoId));

        PlanEstudioFt001JpaEntity plan = planEstudioRepository.findFirstByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(proceso.getCarreraId())
                .orElseThrow(() -> new DomainValidationException("No hay un plan de estudio cargado para la carrera de este proceso"));

        List<ProgramaAnaliticoAsignaturaJpaEntity> existentes = programaRepository.findByProcesoIdOrderByNombreAsignaturaAsc(procesoId);
        Set<String> nombresExistentes = existentes.stream()
                .map(p -> p.getNombreAsignatura().trim().toLowerCase())
                .collect(Collectors.toSet());

        List<ProgramaAnaliticoAsignaturaJpaEntity> aGuardar = new ArrayList<>();
        Instant now = Instant.now();

        for (ActividadCurricularPlanJpaEntity asig : plan.getAsignaturas()) {
            if (!nombresExistentes.contains(asig.getNombre().trim().toLowerCase())) {
                ProgramaAnaliticoAsignaturaJpaEntity prog = new ProgramaAnaliticoAsignaturaJpaEntity();
                prog.setId(UUID.randomUUID());
                prog.setProcesoId(procesoId);
                prog.setActividadCurricular(asig);
                prog.setNombreAsignatura(asig.getNombre());
                prog.setAnioCursada(asig.getSubgrupo());
                prog.setResolucionAprobacion(plan.getCodigoResolucion());
                prog.setEstado("PENDIENTE");
                prog.setCreadoEn(now);
                prog.setActualizadoEn(now);
                aGuardar.add(prog);
            }
        }

        if (!aGuardar.isEmpty()) {
            programaRepository.saveAll(aGuardar);
        }

        List<ProgramaAnaliticoAsignaturaJpaEntity> total = programaRepository.findByProcesoIdOrderByNombreAsignaturaAsc(procesoId);
        return ResponseEntity.ok(total.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<ProgramaAnaliticoAsignaturaDto> subirPrograma(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("asignaturaPlanId") UUID asignaturaPlanId,
            @RequestParam(value = "procesoId", required = false) UUID procesoId,
            @RequestParam(value = "resolucionAprobacion", required = false) String resolucionAprobacion,
            @RequestParam(value = "vigenciaDesde", required = false) Integer vigenciaDesde,
            @RequestParam(value = "vigenciaHasta", required = false) Integer vigenciaHasta,
            @RequestParam(value = "observaciones", required = false) String observaciones
    ) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new DomainValidationException("Debe seleccionar un archivo PDF para el programa analítico");
        }

        ActividadCurricularPlanJpaEntity asig = asignaturaRepository.findById(asignaturaPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignatura del plan con ID: " + asignaturaPlanId));

        UUID effectiveProcesoId = procesoId;
        if (effectiveProcesoId == null) {
            if (asig.getPlanEstudio() != null && asig.getPlanEstudio().getProcesoId() != null) {
                effectiveProcesoId = asig.getPlanEstudio().getProcesoId();
            } else if (asig.getPlanEstudio() != null && asig.getPlanEstudio().getCarrera() != null) {
                effectiveProcesoId = procesoRepository.findByCarreraIdAndEstadoIn(
                        asig.getPlanEstudio().getCarrera().getId(),
                        List.of("EN_PREPARACION", "INICIADO", "CONFIRMADA", "PENDIENTE_DIRECTOR_ESCUELA", "EN_CURSO", "EN_FUNCIONAMIENTO")
                ).map(ProcesoAcreditacionJpaEntity::getId).orElse(null);

                if (effectiveProcesoId == null) {
                    List<ProcesoAcreditacionJpaEntity> procesos = procesoRepository.findAllByOrderByCreadoEnDesc();
                    effectiveProcesoId = procesos.stream()
                            .filter(p -> p.getCarreraId() != null && p.getCarreraId().equals(asig.getPlanEstudio().getCarrera().getId()))
                            .map(ProcesoAcreditacionJpaEntity::getId)
                            .findFirst()
                            .orElse(null);
                }
            }
        }

        Optional<ProgramaAnaliticoAsignaturaJpaEntity> progOpt = programaRepository.findByActividadCurricularId(asig.getId());
        if (progOpt.isEmpty() && effectiveProcesoId != null) {
            progOpt = programaRepository.findByProcesoIdAndNombreAsignatura(effectiveProcesoId, asig.getNombre().trim());
        }

        ProgramaAnaliticoAsignaturaJpaEntity entity = progOpt.orElseGet(ProgramaAnaliticoAsignaturaJpaEntity::new);
        Instant now = Instant.now();
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setCreadoEn(now);
        }

        entity.setProcesoId(effectiveProcesoId);
        entity.setActividadCurricular(asig);
        entity.setNombreAsignatura(asig.getNombre().trim());
        entity.setAnioCursada(asig.getSubgrupo());
        entity.setResolucionAprobacion(resolucionAprobacion != null && !resolucionAprobacion.isBlank()
                ? resolucionAprobacion.trim()
                : (asig.getPlanEstudio() != null ? asig.getPlanEstudio().getCodigoResolucion() : null));
        entity.setVigenciaDesde(vigenciaDesde);
        entity.setVigenciaHasta(vigenciaHasta);
        entity.setObservaciones(observaciones);
        entity.setActualizadoEn(now);

        String originalName = archivo.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".pdf";
        String storageKey = "programas-analiticos/" + (effectiveProcesoId != null ? effectiveProcesoId : asig.getPlanEstudio().getId()) + "/" + entity.getId() + extension;
        String ruta = storagePort.uploadFile(archivo.getBytes(), storageKey, archivo.getContentType() != null ? archivo.getContentType() : "application/pdf");

        entity.setArchivoId(UUID.randomUUID());
        entity.setArchivoNombre(originalName != null ? originalName : "Programa_" + asig.getNombre() + ".pdf");
        entity.setArchivoRuta(ruta);
        entity.setTamanioBytes(archivo.getSize());
        entity.setMimeType(archivo.getContentType() != null ? archivo.getContentType() : "application/pdf");
        entity.setEstado("CARGADO");

        ProgramaAnaliticoAsignaturaJpaEntity guardado = programaRepository.save(entity);
        return ResponseEntity.ok(toDto(guardado));
    }

    @PostMapping(value = "/proceso/{procesoId}/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA', 'COMISION_AUTOEVALUACION') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<ProgramaAnaliticoAsignaturaDto> cargarPrograma(
            @PathVariable UUID procesoId,
            @RequestParam("nombreAsignatura") String nombreAsignatura,
            @RequestParam(value = "anioCursada", required = false) String anioCursada,
            @RequestParam(value = "resolucionAprobacion", required = false) String resolucionAprobacion,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo
    ) throws IOException {
        if (nombreAsignatura == null || nombreAsignatura.isBlank()) {
            throw new DomainValidationException("El nombre de la asignatura es obligatorio");
        }

        Optional<ProgramaAnaliticoAsignaturaJpaEntity> progOpt = programaRepository.findByProcesoIdAndNombreAsignatura(procesoId, nombreAsignatura.trim());
        ProgramaAnaliticoAsignaturaJpaEntity entity = progOpt.orElseGet(ProgramaAnaliticoAsignaturaJpaEntity::new);

        Instant now = Instant.now();
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setProcesoId(procesoId);
            entity.setCreadoEn(now);
        }

        entity.setNombreAsignatura(nombreAsignatura.trim());
        entity.setAnioCursada(anioCursada);
        entity.setResolucionAprobacion(resolucionAprobacion);
        entity.setObservaciones(observaciones);
        entity.setActualizadoEn(now);

        if (archivo != null && !archivo.isEmpty()) {
            String originalName = archivo.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".pdf";
            String storageKey = "procesos/" + procesoId + "/programas-analiticos/" + entity.getId() + extension;
            String ruta = storagePort.uploadFile(archivo.getBytes(), storageKey, archivo.getContentType() != null ? archivo.getContentType() : "application/pdf");

            entity.setArchivoId(UUID.randomUUID());
            entity.setArchivoNombre(originalName != null ? originalName : "Programa_" + nombreAsignatura + ".pdf");
            entity.setArchivoRuta(ruta);
            entity.setTamanioBytes(archivo.getSize());
            entity.setMimeType(archivo.getContentType() != null ? archivo.getContentType() : "application/pdf");
            entity.setEstado("CARGADO");
        }

        ProgramaAnaliticoAsignaturaJpaEntity guardado = programaRepository.save(entity);
        return ResponseEntity.ok(toDto(guardado));
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable UUID id) {
        ProgramaAnaliticoAsignaturaJpaEntity entity = programaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el programa analítico: " + id));

        if (entity.getArchivoRuta() == null || entity.getArchivoRuta().isBlank()) {
            throw new ResourceNotFoundException("El programa analítico no tiene un archivo PDF adjunto");
        }

        byte[] bytes = storagePort.downloadFile(entity.getArchivoRuta());
        ByteArrayResource resource = new ByteArrayResource(bytes);

        String filename = entity.getArchivoNombre() != null ? entity.getArchivoNombre() : "programa_analitico.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/validar-completar-8-4")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<Map<String, Object>> validarYCompletar84(
            @RequestParam(required = false) UUID procesoId,
            @RequestParam(required = false) UUID planId
    ) {
        UUID effectiveProcesoId = procesoId;
        UUID effectivePlanId = planId;

        if (effectiveProcesoId == null && effectivePlanId != null) {
            PlanEstudioFt001JpaEntity plan = planEstudioRepository.findById(effectivePlanId).orElse(null);
            if (plan != null) {
                if (plan.getProcesoId() != null) {
                    effectiveProcesoId = plan.getProcesoId();
                } else if (plan.getCarrera() != null) {
                    effectiveProcesoId = procesoRepository.findByCarreraIdAndEstadoIn(
                            plan.getCarrera().getId(),
                            List.of("EN_PREPARACION", "INICIADO", "CONFIRMADA", "PENDIENTE_DIRECTOR_ESCUELA", "EN_CURSO", "EN_FUNCIONAMIENTO")
                    ).map(ProcesoAcreditacionJpaEntity::getId).orElse(null);
                }
            }
        }

        if (effectivePlanId == null && effectiveProcesoId != null) {
            ProcesoAcreditacionJpaEntity proc = procesoRepository.findById(effectiveProcesoId).orElse(null);
            if (proc != null && proc.getCarreraId() != null) {
                effectivePlanId = planEstudioRepository.findFirstByCarreraIdAndActivoTrueOrderByAnioAprobacionDesc(proc.getCarreraId())
                        .map(PlanEstudioFt001JpaEntity::getId)
                        .orElse(null);
            }
        }

        long total = 0;
        long cargados = 0;

        if (effectivePlanId != null) {
            total = asignaturaRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(effectivePlanId).size();
            List<ProgramaAnaliticoAsignaturaJpaEntity> list = programaRepository.findByPlanEstudioId(effectivePlanId);
            cargados = list.stream().filter(p -> "CARGADO".equalsIgnoreCase(p.getEstado())).count();
        } else if (effectiveProcesoId != null) {
            total = programaRepository.countByProcesoId(effectiveProcesoId);
            cargados = programaRepository.countByProcesoIdAndEstado(effectiveProcesoId, "CARGADO");
        }

        if (total == 0) {
            throw new DomainValidationException("No se han configurado asignaturas para los programas analíticos");
        }
        if (cargados < total) {
            throw new DomainValidationException("No se puede completar la actividad 8.4: faltan programas analíticos por cargar (" + cargados + " de " + total + " cargados).");
        }

        if (effectiveProcesoId != null) {
            Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(effectiveProcesoId);
            if (planOpt.isPresent()) {
                PlanTrabajo plan = planOpt.get();
                Optional<ActividadPlanTrabajo> act84Opt = plan.getActividades().stream()
                        .filter(a -> "8.4".equals(a.getCodigoActividad()))
                        .findFirst();

                if (act84Opt.isPresent()) {
                    ActividadPlanTrabajo act84 = act84Opt.get();
                    UUID usuarioId = resolverUsuarioAutenticadoId();
                    act84.completar(usuarioId, "Programas analíticos en PDF recopilados y legalizados (" + cargados + " asignaturas)");
                    planTrabajoRepository.saveActividad(act84);
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", "Actividad 8.4 completada exitosamente.",
                "totalAsignaturas", total,
                "programasCargados", cargados
        ));
    }

    @PostMapping("/proceso/{procesoId}/completar-actividad-8-4")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'DIRECTOR_CARRERA') or hasAuthority('CONVOCATORIA_MANAGE')")
    @Transactional
    public ResponseEntity<Map<String, Object>> completarActividad84(@PathVariable UUID procesoId) {
        long total = programaRepository.countByProcesoId(procesoId);
        long cargados = programaRepository.countByProcesoIdAndEstado(procesoId, "CARGADO");

        if (total == 0) {
            throw new DomainValidationException("No se han configurado materias para este proceso de acreditación");
        }
        if (cargados < total) {
            throw new DomainValidationException("No se puede completar la actividad 8.4: faltan programas analíticos por cargar (" + cargados + " de " + total + " cargados).");
        }

        Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(procesoId);
        if (planOpt.isPresent()) {
            PlanTrabajo plan = planOpt.get();
            Optional<ActividadPlanTrabajo> act84Opt = plan.getActividades().stream()
                    .filter(a -> "8.4".equals(a.getCodigoActividad()))
                    .findFirst();

            if (act84Opt.isPresent()) {
                ActividadPlanTrabajo act84 = act84Opt.get();
                UUID usuarioId = resolverUsuarioAutenticadoId();
                act84.completar(usuarioId, "Programas analíticos en PDF recopilados y legalizados (" + cargados + " asignaturas)");
                planTrabajoRepository.saveActividad(act84);
            }
        }

        return ResponseEntity.ok(Map.of(
                "mensaje", "Actividad 8.4 completada exitosamente.",
                "totalAsignaturas", total,
                "programasCargados", cargados
        ));
    }

    private UUID resolverUsuarioAutenticadoId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }

    private ProgramaAnaliticoAsignaturaDto toDto(ProgramaAnaliticoAsignaturaJpaEntity entity) {
        UUID asigId = entity.getActividadCurricular() != null ? entity.getActividadCurricular().getId() : null;
        String asigCodigo = entity.getActividadCurricular() != null ? entity.getActividadCurricular().getCodigo() : null;
        String archivoUrl = entity.getId() != null ? "/api/programas-analiticos/" + entity.getId() + "/descargar" : null;

        return new ProgramaAnaliticoAsignaturaDto(
                entity.getId(),
                entity.getProcesoId(),
                asigId,
                asigId, // asignaturaPlanId alias
                asigCodigo, // asignaturaCodigo alias
                entity.getNombreAsignatura(),
                entity.getNombreAsignatura(), // asignaturaNombre alias
                entity.getAnioCursada(),
                entity.getResolucionAprobacion(),
                entity.getVigenciaDesde(),
                entity.getVigenciaHasta(),
                entity.getArchivoId(),
                entity.getArchivoNombre(),
                entity.getArchivoRuta(),
                archivoUrl,
                entity.getTamanioBytes(),
                entity.getMimeType(),
                entity.getEstado(),
                entity.getEstado(), // estadoCarga alias ('CARGADO' / 'PENDIENTE')
                entity.getObservaciones(),
                entity.getCreadoEn(),
                entity.getActualizadoEn()
        );
    }
}
