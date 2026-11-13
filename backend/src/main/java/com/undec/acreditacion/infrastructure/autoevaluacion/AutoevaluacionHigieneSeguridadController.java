package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.GuiaHigieneSeguridadClonerService;
import com.undec.acreditacion.infrastructure.persistence.postgres.InstitucionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataInstitucionRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/autoevaluacion/higiene-seguridad")
public class AutoevaluacionHigieneSeguridadController {

    private final SpringDataGuiaHigieneSeguridadRepository guiaRepository;
    private final SpringDataGuiaHigieneDocumentoRepository documentoRepository;
    private final SpringDataInstitucionRepository institucionRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final GuiaHigieneSeguridadClonerService clonerService;

    public AutoevaluacionHigieneSeguridadController(
            SpringDataGuiaHigieneSeguridadRepository guiaRepository,
            SpringDataGuiaHigieneDocumentoRepository documentoRepository,
            SpringDataInstitucionRepository institucionRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            GuiaHigieneSeguridadClonerService clonerService
    ) {
        this.guiaRepository = guiaRepository;
        this.documentoRepository = documentoRepository;
        this.institucionRepository = institucionRepository;
        this.planTrabajoRepository = planTrabajoRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.clonerService = clonerService;
    }

    // -------------------------------------------------------------------------
    // 1. Guía Maestra Institucional (GT-003)
    // -------------------------------------------------------------------------

    @GetMapping("/maestro")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'AEA', 'HIGIENE_SEGURIDAD', 'INFRAESTRUCTURA', 'BEDELIA', 'CAREER_DIRECTOR', 'DIRECTOR_CARRERA', 'SCHOOL_DIRECTOR', 'DIRECTOR_ESCUELA', 'SELF_EVALUATION_COMMITTEE_MEMBER', 'COMISION_AUTOEVALUACION', 'AC_COORDINATOR', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<GuiaHigieneSeguridadResponse> obtenerGuiaMaestra() {
        return guiaRepository.findGuiaMaestraCompleta()
                .map(this::mapToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------
    // 2. Guía por Proceso / Carrera (Snapshot de Autoevaluación)
    // -------------------------------------------------------------------------

    @GetMapping("/procesos/{procesoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'AEA', 'HIGIENE_SEGURIDAD', 'INFRAESTRUCTURA', 'BEDELIA', 'CAREER_DIRECTOR', 'DIRECTOR_CARRERA', 'SCHOOL_DIRECTOR', 'DIRECTOR_ESCUELA', 'SELF_EVALUATION_COMMITTEE_MEMBER', 'COMISION_AUTOEVALUACION', 'AC_COORDINATOR', 'DOCENTE')")
    public ResponseEntity<GuiaHigieneSeguridadResponse> obtenerGuiaPorProceso(@PathVariable UUID procesoId) {
        Optional<GuiaHigieneSeguridadJpaEntity> opt = guiaRepository.findByProcesoIdCompleta(procesoId);
        if (opt.isPresent()) {
            return ResponseEntity.ok(mapToResponse(opt.get()));
        }

        try {
            GuiaHigieneSeguridadJpaEntity guia = clonerService.clonarGuiaMaestraParaProceso(procesoId);
            return ResponseEntity.ok(mapToResponse(guia));
        } catch (DataIntegrityViolationException ex) {
            // Concurrencia: otro hilo o petición clonó la guía simultáneamente
            GuiaHigieneSeguridadJpaEntity reloaded = guiaRepository.findByProcesoIdCompleta(procesoId)
                    .orElseThrow(() -> ex);
            return ResponseEntity.ok(mapToResponse(reloaded));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'HIGIENE_SEGURIDAD')")
    @Transactional
    public ResponseEntity<GuiaHigieneSeguridadResponse> actualizarGuia(
            @PathVariable UUID id,
            @RequestBody ActualizarGuiaHigieneRequest request
    ) {
        Optional<GuiaHigieneSeguridadJpaEntity> opt = guiaRepository.findByIdCompleta(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GuiaHigieneSeguridadJpaEntity entity = opt.get();
        if (request.instanciaResponsableUa() != null) {
            entity.setInstanciaResponsableUa(request.instanciaResponsableUa());
        }
        if (request.politicaUnidadesApoyo() != null) {
            entity.setPoliticaUnidadesApoyo(request.politicaUnidadesApoyo());
        }
        if (request.aplicaUnidadesApoyo() != null) {
            entity.setAplicaUnidadesApoyo(request.aplicaUnidadesApoyo());
        }
        if (request.observaciones() != null) {
            entity.setObservaciones(request.observaciones());
        }
        entity.setActualizadoEn(Instant.now());

        GuiaHigieneSeguridadJpaEntity guardado = guiaRepository.save(entity);
        return ResponseEntity.ok(mapToResponse(guardado));
    }

    @PutMapping("/procesos/{procesoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'HIGIENE_SEGURIDAD')")
    @Transactional
    public ResponseEntity<GuiaHigieneSeguridadResponse> actualizarGuiaPorProceso(
            @PathVariable UUID procesoId,
            @RequestBody ActualizarGuiaHigieneRequest request
    ) {
        Optional<GuiaHigieneSeguridadJpaEntity> opt = guiaRepository.findByProcesoIdCompleta(procesoId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GuiaHigieneSeguridadJpaEntity entity = opt.get();
        if (request.instanciaResponsableUa() != null) {
            entity.setInstanciaResponsableUa(request.instanciaResponsableUa());
        }
        if (request.politicaUnidadesApoyo() != null) {
            entity.setPoliticaUnidadesApoyo(request.politicaUnidadesApoyo());
        }
        if (request.aplicaUnidadesApoyo() != null) {
            entity.setAplicaUnidadesApoyo(request.aplicaUnidadesApoyo());
        }
        if (request.observaciones() != null) {
            entity.setObservaciones(request.observaciones());
        }
        entity.setActualizadoEn(Instant.now());

        GuiaHigieneSeguridadJpaEntity guardado = guiaRepository.save(entity);
        return ResponseEntity.ok(mapToResponse(guardado));
    }

    // -------------------------------------------------------------------------
    // 3. Documentación de Respaldo y Normativa MinIO
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/documentos")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'HIGIENE_SEGURIDAD')")
    @Transactional
    public ResponseEntity<GuiaHigieneDocumentoResponse> agregarOActualizarDocumento(
            @PathVariable UUID id,
            @RequestBody DocumentoHigieneItemRequest request
    ) {
        Optional<GuiaHigieneSeguridadJpaEntity> opt = guiaRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GuiaHigieneSeguridadJpaEntity guia = opt.get();
        GuiaHigieneDocumentoJpaEntity doc;

        if (request.id() != null) {
            doc = documentoRepository.findById(request.id())
                    .orElse(new GuiaHigieneDocumentoJpaEntity());
        } else {
            doc = new GuiaHigieneDocumentoJpaEntity();
            doc.setId(UUID.randomUUID());
            doc.setGuia(guia);
            doc.setCreadoEn(Instant.now());
        }

        doc.setTipoDocumentoClave(request.tipoDocumentoClave() != null ? request.tipoDocumentoClave() : "NORMATIVA_RESPALDO");
        doc.setTipoDocumentoNombre(request.tipoDocumentoNombre() != null ? request.tipoDocumentoNombre() : "Documento de Respaldo");
        doc.setOrganismoEmisor(request.organismoEmisor());
        doc.setNumeroNormativa(request.numeroNormativa());
        doc.setDescripcion(request.descripcion());
        doc.setNombreArchivo(request.nombreArchivo());
        doc.setUrlMinio(request.urlMinio() != null ? request.urlMinio() :
                (request.nombreArchivo() != null ? "/minio/acreditacion/03_01/" + request.nombreArchivo() : null));
        doc.setRutaRepositorio("03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa");
        doc.setObligatorio(request.obligatorio() != null ? request.obligatorio() : false);
        doc.setCargado(request.nombreArchivo() != null && !request.nombreArchivo().isBlank());
        doc.setSubidoPorUsuarioId(resolverUsuarioAutenticadoId());
        doc.setFechaSubida(Instant.now());

        GuiaHigieneDocumentoJpaEntity guardado = documentoRepository.save(doc);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDocumentoResponse(guardado));
    }

    // -------------------------------------------------------------------------
    // 4. Extensión de Plazo sincronizada con PT-001 (Actividad 4.2)
    // -------------------------------------------------------------------------

    @PostMapping("/procesos/{procesoId}/extender-plazo")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'AEA')")
    @Transactional
    public ResponseEntity<Map<String, Object>> extenderPlazoActividad(
            @PathVariable UUID procesoId,
            @RequestBody ExtenderPlazoInfraestructuraRequest request
    ) {
        if (request.nuevaFechaLimite() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "La nueva fecha límite es obligatoria"));
        }

        Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(procesoId);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PlanTrabajo plan = planOpt.get();
        Optional<ActividadPlanTrabajo> actOpt = plan.getActividades().stream()
                .filter(a -> "4.2".equals(a.getCodigoActividad()) || a.getNombre().toLowerCase().contains("higiene"))
                .findFirst();

        if (actOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se encontró la actividad 4.2 de Higiene y Seguridad en el Plan de Trabajo"));
        }

        ActividadPlanTrabajo act = actOpt.get();
        act.agregarPlazoExtendido(request.nuevaFechaLimite());
        planTrabajoRepository.saveActividad(act);
        planTrabajoRepository.save(plan);

        // Actualizar fecha límite en la entidad GT-003 si existe
        guiaRepository.findByProcesoIdCompleta(procesoId).ifPresent(g -> {
            g.setFechaLimite(request.nuevaFechaLimite());
            g.setActualizadoEn(Instant.now());
            guiaRepository.save(g);
        });

        Map<String, Object> resp = new HashMap<>();
        resp.put("procesoId", procesoId);
        resp.put("codigoActividad", act.getCodigoActividad());
        resp.put("nuevaFechaLimite", act.getUltimoPlazoVigente());
        resp.put("plazosExtendidos", act.getPlazosExtendidos());
        resp.put("motivo", request.motivo());
        resp.put("mensaje", "Plazo extendido exitosamente y sincronizado con el Plan de Trabajo (hoja INFO - Actividad 4.2)");
        return ResponseEntity.ok(resp);
    }

    // -------------------------------------------------------------------------
    // 5. Verificación y Remisión a AEA (Completar Tarea 4.2)
    // -------------------------------------------------------------------------

    @PostMapping("/procesos/{procesoId}/verificar-y-remitir")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'HIGIENE_SEGURIDAD')")
    @Transactional
    public ResponseEntity<VerificarInfraestructuraResponse> verificarYRemitirADea(
            @PathVariable UUID procesoId
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        Instant now = Instant.now();

        // 1. Marcar la GT-003 del proceso como verificada
        Optional<GuiaHigieneSeguridadJpaEntity> guiaOpt = guiaRepository.findByProcesoIdCompleta(procesoId);
        if (guiaOpt.isPresent()) {
            GuiaHigieneSeguridadJpaEntity g = guiaOpt.get();
            g.setVerificado(true);
            g.setFechaVerificacion(now);
            g.setEstado("COMPLETADO");
            g.setActualizadoEn(now);
            guiaRepository.save(g);
        }

        // 2. Actualizar automáticamente la actividad 4.2 en el Plan de Trabajo (PT-001)
        String estadoActividad = "SIN_PLAN";
        Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(procesoId);
        if (planOpt.isPresent()) {
            PlanTrabajo plan = planOpt.get();
            Optional<ActividadPlanTrabajo> actOpt = plan.getActividades().stream()
                    .filter(a -> "4.2".equals(a.getCodigoActividad()) || a.getNombre().toLowerCase().contains("higiene"))
                    .findFirst();

            if (actOpt.isPresent()) {
                ActividadPlanTrabajo act = actOpt.get();
                act.completar(usuarioId, "GT-003 y documentación de respaldo de Higiene y Seguridad remitida a AEA y archivada en subcarpeta Normativa");
                planTrabajoRepository.saveActividad(act);
                planTrabajoRepository.save(plan);
                estadoActividad = act.getEstado().name();
            }
        }

        return ResponseEntity.ok(new VerificarInfraestructuraResponse(
                procesoId,
                true,
                estadoActividad,
                now,
                "Guía GT-003 y documentación de respaldo verificadas. Tarea 4.2 finalizada y asentada en hoja INFO del PT-001"
        ));
    }

    private GuiaHigieneSeguridadResponse mapToResponse(GuiaHigieneSeguridadJpaEntity g) {
        List<GuiaHigieneDocumentoResponse> docs = g.getDocumentos() == null ? Collections.emptyList() :
                g.getDocumentos().stream()
                        .map(this::mapToDocumentoResponse)
                        .collect(Collectors.toList());

        int cargados = (int) docs.stream().filter(d -> Boolean.TRUE.equals(d.cargado())).count();
        int obligatorios = (int) docs.stream().filter(d -> Boolean.TRUE.equals(d.obligatorio())).count();
        int obligatoriosCargados = (int) docs.stream()
                .filter(d -> Boolean.TRUE.equals(d.obligatorio()) && Boolean.TRUE.equals(d.cargado()))
                .count();

        boolean requisitosCompletos = obligatoriosCargados >= obligatorios && obligatorios > 0;

        return new GuiaHigieneSeguridadResponse(
                g.getId(),
                g.getProcesoId(),
                g.getEsMaestro(),
                g.getEdicion(),
                g.getFechaVigencia(),
                g.getElaboradoPor(),
                g.getRevisadoPor(),
                g.getAprobadoPor(),
                g.getInstanciaResponsableUa(),
                g.getPoliticaUnidadesApoyo(),
                g.getAplicaUnidadesApoyo(),
                g.getVerificado(),
                g.getFechaVerificacion(),
                g.getFechaSolicitud(),
                g.getFechaLimite(),
                g.getEstado(),
                g.getObservaciones(),
                docs,
                cargados,
                obligatorios,
                requisitosCompletos
        );
    }

    private GuiaHigieneDocumentoResponse mapToDocumentoResponse(GuiaHigieneDocumentoJpaEntity d) {
        return new GuiaHigieneDocumentoResponse(
                d.getId(),
                d.getTipoDocumentoClave(),
                d.getTipoDocumentoNombre(),
                d.getOrganismoEmisor(),
                d.getNumeroNormativa(),
                d.getDescripcion(),
                d.getNombreArchivo(),
                d.getUrlMinio(),
                d.getRutaRepositorio(),
                d.getObligatorio(),
                d.getCargado(),
                d.getFechaSubida()
        );
    }

    private UUID resolverUsuarioAutenticadoId() {
        return authenticatedUserProvider.getAuthenticatedUserIdOrFallback();
    }
}
