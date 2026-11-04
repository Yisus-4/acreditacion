package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/autoevaluacion")
public class AutoevaluacionInfraestructuraController {

    private final SpringDataInmuebleRepository inmuebleRepository;
    private final SpringDataTipoEspacioFisicoRepository tipoEspacioRepository;
    private final SpringDataEspacioFisicoRepository espacioFisicoRepository;
    private final SpringDataInmuebleDocumentacionRespaldoRepository documentacionRepository;
    private final SpringDataInstitucionRepository institucionRepository;
    private final SpringDataProvinciaRepository provinciaRepository;
    private final SpringDataDepartamentoRepository departamentoRepository;
    private final SpringDataLocalidadRepository localidadRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final UserRepository userRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;

    public AutoevaluacionInfraestructuraController(
            SpringDataInmuebleRepository inmuebleRepository,
            SpringDataTipoEspacioFisicoRepository tipoEspacioRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataInmuebleDocumentacionRespaldoRepository documentacionRepository,
            SpringDataInstitucionRepository institucionRepository,
            SpringDataProvinciaRepository provinciaRepository,
            SpringDataDepartamentoRepository departamentoRepository,
            SpringDataLocalidadRepository localidadRepository,
            SpringDataCarreraRepository carreraRepository,
            UserRepository userRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository
    ) {
        this.inmuebleRepository = inmuebleRepository;
        this.tipoEspacioRepository = tipoEspacioRepository;
        this.espacioFisicoRepository = espacioFisicoRepository;
        this.documentacionRepository = documentacionRepository;
        this.institucionRepository = institucionRepository;
        this.provinciaRepository = provinciaRepository;
        this.departamentoRepository = departamentoRepository;
        this.localidadRepository = localidadRepository;
        this.carreraRepository = carreraRepository;
        this.userRepository = userRepository;
        this.planTrabajoRepository = planTrabajoRepository;
    }

    // -------------------------------------------------------------------------
    // Inmuebles - Padrón General y Maestro
    // -------------------------------------------------------------------------

    @GetMapping("/inmuebles")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InmuebleResponse>> listarInmuebles(
            @RequestParam(name = "esMaestro", required = false) Boolean esMaestro
    ) {
        List<InmuebleJpaEntity> entidades;
        if (Boolean.TRUE.equals(esMaestro)) {
            entidades = inmuebleRepository.findMaestrosActivos();
        } else {
            entidades = inmuebleRepository.findAllActivosConRelaciones();
        }
        List<InmuebleResponse> response = entidades.stream()
                .map(this::mapToInmuebleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inmuebles/maestro")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InmuebleResponse>> listarInmueblesMaestros() {
        List<InmuebleJpaEntity> entidades = inmuebleRepository.findMaestrosActivos();
        List<InmuebleResponse> response = entidades.stream()
                .map(this::mapToInmuebleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inmuebles/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<InmuebleResponse> obtenerInmueble(@PathVariable UUID id) {
        return inmuebleRepository.findByIdCompleto(id)
                .map(this::mapToInmuebleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/inmuebles")
    @Transactional
    public ResponseEntity<InmuebleResponse> registrarInmueble(@RequestBody RegistrarInmuebleRequest request) {
        InstitucionJpaEntity institucion = institucionRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe institución registrada en el sistema"));

        InmuebleJpaEntity entity = new InmuebleJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setInstitucion(institucion);
        entity.setNumeroOrden(request.numeroOrden());
        entity.setNombre(request.nombre());
        entity.setDomicilio(request.domicilio() != null ? request.domicilio() : (request.calle() + " " + request.numero()));
        entity.setCalle(request.calle());
        entity.setNumero(request.numero());
        entity.setAnioConstruccion(request.anioConstruccion());
        entity.setAnioInicioInstitucion(request.anioInicioInstitucion());
        entity.setPropiedadInstitucion(request.propiedadInstitucion() != null ? request.propiedadInstitucion() : true);
        entity.setCondicionTenencia(request.condicionTenencia() != null ? request.condicionTenencia().toUpperCase() : "PROPIO");
        entity.setSituacion(request.situacion() != null ? request.situacion() : "Propiedad");
        entity.setVinculaTodasCarreras(request.vinculaTodasCarreras() != null ? request.vinculaTodasCarreras() : false);
        // Si no se asigna a un proceso particular, pertenece siempre al padrón maestro para no quedar huérfano
        boolean esMaestro = (request.procesoId() == null) || Boolean.TRUE.equals(request.esMaestro());
        entity.setEsMaestro(esMaestro);
        entity.setProcesoId(request.procesoId());
        entity.setSuperficieCubiertaM2(request.superficieCubiertaM2());
        entity.setSuperficieTotalM2(request.superficieTotalM2());
        entity.setObservaciones(request.observaciones());
        entity.setEstadoConeau(request.estadoConeau() != null ? request.estadoConeau() : "CARGADO");
        entity.setVerificado(false);
        entity.setActivo(true);
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        if (request.provinciaId() != null) {
            provinciaRepository.findById(request.provinciaId()).ifPresent(entity::setProvincia);
        }
        if (request.departamentoId() != null) {
            departamentoRepository.findById(request.departamentoId()).ifPresent(entity::setDepartamentoGeografico);
        }
        if (request.localidadId() != null) {
            localidadRepository.findById(request.localidadId()).ifPresent(entity::setLocalidad);
        }

        if (request.carrerasIds() != null && !request.carrerasIds().isEmpty()) {
            Set<CarreraJpaEntity> carreras = new HashSet<>(carreraRepository.findAllById(request.carrerasIds()));
            entity.setCarreras(carreras);
        }

        InmuebleJpaEntity guardado = inmuebleRepository.save(entity);

        // Espacios físicos
        if (request.espacios() != null && !request.espacios().isEmpty()) {
            for (EspacioFisicoRequest espReq : request.espacios()) {
                EspacioFisicoJpaEntity esp = new EspacioFisicoJpaEntity();
                esp.setId(UUID.randomUUID());
                esp.setInmueble(guardado);
                esp.setDenominacion(espReq.denominacion());
                esp.setDescripcionInfraestructura(espReq.descripcionInfraestructura());
                esp.setCantidad(espReq.cantidad() != null ? espReq.cantidad() : 1);
                int cap = espReq.capacidadPersonasSentadas() != null ? espReq.capacidadPersonasSentadas() :
                        (espReq.capacidadEstudiantes() != null ? espReq.capacidadEstudiantes() : 0);
                esp.setCapacidadPersonasSentadas(cap);
                esp.setCapacidadEstudiantes(cap);
                esp.setSuperficieM2(espReq.superficieM2());
                esp.setEquipamientoDisponible(espReq.equipamientoDisponible());
                esp.setEstadoConeau(espReq.estadoConeau() != null ? espReq.estadoConeau() : "CARGADO");
                esp.setActivo(true);
                esp.setCreadoEn(Instant.now());
                if (espReq.tipoEspacioId() != null) {
                    tipoEspacioRepository.findById(espReq.tipoEspacioId()).ifPresent(esp::setTipoEspacio);
                }
                espacioFisicoRepository.save(esp);
                guardado.getEspacios().add(esp);
            }
        }

        // Documentos de respaldo (MinIO)
        if (request.documentos() != null && !request.documentos().isEmpty()) {
            for (RegistrarInmuebleRequest.DocumentoRespaldoRequest docReq : request.documentos()) {
                InmuebleDocumentacionRespaldoJpaEntity doc = new InmuebleDocumentacionRespaldoJpaEntity();
                doc.setId(UUID.randomUUID());
                doc.setInmueble(guardado);
                doc.setNombreArchivo(docReq.nombreArchivo());
                doc.setUrlMinio(docReq.urlMinio());
                doc.setTipoAnexo(docReq.tipoAnexo());
                doc.setDescripcion(docReq.descripcion());
                doc.setRutaRepositorio("03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa");
                doc.setFechaSubida(Instant.now());
                documentacionRepository.save(doc);
                guardado.getDocumentos().add(doc);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToInmuebleResponse(guardado));
    }

    @PutMapping("/inmuebles/{id}")
    @Transactional
    public ResponseEntity<InmuebleResponse> actualizarInmueble(
            @PathVariable UUID id,
            @RequestBody RegistrarInmuebleRequest request
    ) {
        Optional<InmuebleJpaEntity> opt = inmuebleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InmuebleJpaEntity entity = opt.get();
        if (request.numeroOrden() != null) entity.setNumeroOrden(request.numeroOrden());
        entity.setNombre(request.nombre());
        entity.setDomicilio(request.domicilio() != null ? request.domicilio() : (request.calle() + " " + request.numero()));
        if (request.calle() != null) entity.setCalle(request.calle());
        if (request.numero() != null) entity.setNumero(request.numero());
        if (request.anioConstruccion() != null) entity.setAnioConstruccion(request.anioConstruccion());
        if (request.anioInicioInstitucion() != null) entity.setAnioInicioInstitucion(request.anioInicioInstitucion());
        entity.setPropiedadInstitucion(request.propiedadInstitucion() != null ? request.propiedadInstitucion() : true);
        entity.setCondicionTenencia(request.condicionTenencia() != null ? request.condicionTenencia().toUpperCase() : "PROPIO");
        if (request.situacion() != null) entity.setSituacion(request.situacion());
        if (request.vinculaTodasCarreras() != null) entity.setVinculaTodasCarreras(request.vinculaTodasCarreras());
        entity.setSuperficieCubiertaM2(request.superficieCubiertaM2());
        entity.setSuperficieTotalM2(request.superficieTotalM2());
        entity.setObservaciones(request.observaciones());
        if (request.estadoConeau() != null) entity.setEstadoConeau(request.estadoConeau());
        entity.setActualizadoEn(Instant.now());

        if (request.provinciaId() != null) {
            provinciaRepository.findById(request.provinciaId()).ifPresent(entity::setProvincia);
        }
        if (request.departamentoId() != null) {
            departamentoRepository.findById(request.departamentoId()).ifPresent(entity::setDepartamentoGeografico);
        }
        if (request.localidadId() != null) {
            localidadRepository.findById(request.localidadId()).ifPresent(entity::setLocalidad);
        }

        if (request.carrerasIds() != null) {
            Set<CarreraJpaEntity> carreras = new HashSet<>(carreraRepository.findAllById(request.carrerasIds()));
            entity.setCarreras(carreras);
        }

        // Espacios físicos - Actualizar, Crear y Dar de baja según CU y Diagrama de Secuencia
        if (request.espacios() != null) {
            List<EspacioFisicoJpaEntity> actuales = espacioFisicoRepository.findByInmuebleIdAndActivoTrue(entity.getId());
            Set<UUID> idsEnviados = request.espacios().stream()
                    .map(EspacioFisicoRequest::id)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // Dar de baja los espacios que ya no están en la lista (Dar de baja espacio físico)
            for (EspacioFisicoJpaEntity act : actuales) {
                if (!idsEnviados.contains(act.getId())) {
                    act.setActivo(false);
                    espacioFisicoRepository.save(act);
                }
            }

            // Actualizar o crear
            for (EspacioFisicoRequest espReq : request.espacios()) {
                EspacioFisicoJpaEntity esp;
                if (espReq.id() != null) {
                    esp = espacioFisicoRepository.findById(espReq.id()).orElse(new EspacioFisicoJpaEntity());
                    if (esp.getId() == null) {
                        esp.setId(espReq.id());
                        esp.setCreadoEn(Instant.now());
                    }
                } else {
                    esp = new EspacioFisicoJpaEntity();
                    esp.setId(UUID.randomUUID());
                    esp.setCreadoEn(Instant.now());
                }
                esp.setInmueble(entity);
                esp.setDenominacion(espReq.denominacion());
                esp.setDescripcionInfraestructura(espReq.descripcionInfraestructura() != null ? espReq.descripcionInfraestructura() : espReq.denominacion());
                esp.setCantidad(espReq.cantidad() != null ? espReq.cantidad() : 1);
                int cap = espReq.capacidadPersonasSentadas() != null ? espReq.capacidadPersonasSentadas() :
                        (espReq.capacidadEstudiantes() != null ? espReq.capacidadEstudiantes() : 0);
                esp.setCapacidadPersonasSentadas(cap);
                esp.setCapacidadEstudiantes(cap);
                esp.setSuperficieM2(espReq.superficieM2());
                esp.setEquipamientoDisponible(espReq.equipamientoDisponible());
                esp.setEstadoConeau(espReq.estadoConeau() != null ? espReq.estadoConeau() : "CARGADO");
                esp.setActivo(true);
                if (espReq.tipoEspacioId() != null) {
                    tipoEspacioRepository.findById(espReq.tipoEspacioId()).ifPresent(esp::setTipoEspacio);
                } else {
                    esp.setTipoEspacio(null);
                }
                espacioFisicoRepository.save(esp);
            }
        }

        // Documentación de respaldo (Adjuntar Documentos en Actualizar Inmueble)
        if (request.documentos() != null && !request.documentos().isEmpty()) {
            List<InmuebleDocumentacionRespaldoJpaEntity> docsExistentes = documentacionRepository.findByInmuebleIdOrderByFechaSubidaDesc(entity.getId());
            Set<String> nombresExistentes = docsExistentes.stream()
                    .map(InmuebleDocumentacionRespaldoJpaEntity::getNombreArchivo)
                    .collect(Collectors.toSet());

            for (RegistrarInmuebleRequest.DocumentoRespaldoRequest docReq : request.documentos()) {
                if (docReq.nombreArchivo() != null && !docReq.nombreArchivo().isBlank() && !nombresExistentes.contains(docReq.nombreArchivo())) {
                    InmuebleDocumentacionRespaldoJpaEntity doc = new InmuebleDocumentacionRespaldoJpaEntity();
                    doc.setId(UUID.randomUUID());
                    doc.setInmueble(entity);
                    doc.setNombreArchivo(docReq.nombreArchivo());
                    doc.setUrlMinio(docReq.urlMinio() != null ? docReq.urlMinio() : ("/minio/acreditacion/03_01/" + docReq.nombreArchivo()));
                    doc.setTipoAnexo(docReq.tipoAnexo() != null ? docReq.tipoAnexo() : "CONVENIO_COMODATO");
                    doc.setDescripcion(docReq.descripcion());
                    doc.setRutaRepositorio("03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa");
                    doc.setFechaSubida(Instant.now());
                    documentacionRepository.save(doc);
                }
            }
        }

        InmuebleJpaEntity guardado = inmuebleRepository.save(entity);
        return inmuebleRepository.findByIdCompleto(guardado.getId())
                .map(this::mapToInmuebleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(mapToInmuebleResponse(guardado)));
    }

    @PostMapping("/inmuebles/{id}/verificar")
    @Transactional
    public ResponseEntity<InmuebleResponse> verificarYRemitirADea(@PathVariable UUID id) {
        Optional<InmuebleJpaEntity> opt = inmuebleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InmuebleJpaEntity entity = opt.get();
        entity.setVerificado(true);
        entity.setFechaVerificacion(Instant.now());
        entity.setActualizadoEn(Instant.now());
        inmuebleRepository.save(entity);

        return ResponseEntity.ok(mapToInmuebleResponse(entity));
    }

    @DeleteMapping("/inmuebles/{id}")
    @Transactional
    public ResponseEntity<Void> eliminarInmueble(@PathVariable UUID id) {
        Optional<InmuebleJpaEntity> opt = inmuebleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InmuebleJpaEntity entity = opt.get();
        entity.setActivo(false);
        inmuebleRepository.save(entity);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Gestión por Proceso de Acreditación (Snapshot + Plan de Trabajo)
    // -------------------------------------------------------------------------

    @GetMapping("/procesos/{procesoId}/inmuebles")
    @Transactional
    public ResponseEntity<?> obtenerInmueblesProceso(@PathVariable UUID procesoId) {
        try {
            List<InmuebleJpaEntity> lista = inmuebleRepository.findByProcesoIdActivos(procesoId);

            // Si aún no tiene snapshot, clonar del padrón maestro
            if (lista.isEmpty()) {
                lista = clonarInmueblesMaestrosParaProceso(procesoId);
            }

            List<InmuebleResponse> response = lista.stream()
                    .map(this::mapToInmuebleResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", ex.getClass().getName() + ": " + ex.getMessage(),
                    "cause", String.valueOf(ex.getCause())
            ));
        }
    }

    @PostMapping("/procesos/{procesoId}/inmuebles/copiar-maestro")
    @Transactional
    public ResponseEntity<List<InmuebleResponse>> regenerarSnapshotDesdeMaestro(@PathVariable UUID procesoId) {
        // Desactivar snapshots anteriores si los hubiera
        List<InmuebleJpaEntity> viejos = inmuebleRepository.findByProcesoIdActivos(procesoId);
        for (InmuebleJpaEntity v : viejos) {
            v.setActivo(false);
            inmuebleRepository.save(v);
        }

        List<InmuebleJpaEntity> nuevos = clonarInmueblesMaestrosParaProceso(procesoId);
        List<InmuebleResponse> response = nuevos.stream()
                .map(this::mapToInmuebleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/procesos/{procesoId}/infraestructura/extender-plazo")
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
                .filter(a -> "4.1".equals(a.getCodigoActividad()) || a.getNombre().toLowerCase().contains("infraestructura"))
                .findFirst();

        if (actOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se encontró la actividad 4.1 de Infraestructura en el Plan de Trabajo"));
        }

        ActividadPlanTrabajo act = actOpt.get();
        act.agregarPlazoExtendido(request.nuevaFechaLimite());
        planTrabajoRepository.saveActividad(act);
        planTrabajoRepository.save(plan);

        Map<String, Object> resp = new HashMap<>();
        resp.put("procesoId", procesoId);
        resp.put("codigoActividad", act.getCodigoActividad());
        resp.put("nuevaFechaLimite", act.getUltimoPlazoVigente());
        resp.put("plazosExtendidos", act.getPlazosExtendidos());
        resp.put("motivo", request.motivo());
        resp.put("mensaje", "Plazo extendido exitosamente y sincronizado con el Plan de Trabajo (hoja INFO)");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/procesos/{procesoId}/infraestructura/verificar-y-remitir")
    @Transactional
    public ResponseEntity<VerificarInfraestructuraResponse> verificarYRemitirADeaPorProceso(
            @PathVariable UUID procesoId
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();

        // 1. Marcar los inmuebles del proceso como verificados
        List<InmuebleJpaEntity> inmuebles = inmuebleRepository.findByProcesoIdActivos(procesoId);
        Instant now = Instant.now();
        for (InmuebleJpaEntity inm : inmuebles) {
            inm.setVerificado(true);
            inm.setFechaVerificacion(now);
            inm.setActualizadoEn(now);
            inmuebleRepository.save(inm);
        }

        // 2. Actualizar automáticamente la actividad 4.1 en el Plan de Trabajo (PT-001)
        String estadoActividad = "SIN_PLAN";
        Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(procesoId);
        if (planOpt.isPresent()) {
            PlanTrabajo plan = planOpt.get();
            Optional<ActividadPlanTrabajo> actOpt = plan.getActividades().stream()
                    .filter(a -> "4.1".equals(a.getCodigoActividad()) || a.getNombre().toLowerCase().contains("infraestructura"))
                    .findFirst();

            if (actOpt.isPresent()) {
                ActividadPlanTrabajo act = actOpt.get();
                act.completar(usuarioId, "Verificación de inmuebles completada por Infraestructura y remitida a AEA");
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
                "Inmuebles verificados exitosamente. Tarea 4.1 finalizada y registrada al 100% en el Plan de Trabajo (hoja INFO)"
        ));
    }

    private List<InmuebleJpaEntity> clonarInmueblesMaestrosParaProceso(UUID procesoId) {
        List<InmuebleJpaEntity> maestros = inmuebleRepository.findMaestrosActivos();
        List<InmuebleJpaEntity> clones = new ArrayList<>();

        for (InmuebleJpaEntity m : maestros) {
            InmuebleJpaEntity c = new InmuebleJpaEntity();
            c.setId(UUID.randomUUID());
            c.setInstitucion(m.getInstitucion());
            c.setNumeroOrden(m.getNumeroOrden());
            c.setNombre(m.getNombre());
            c.setDomicilio(m.getDomicilio());
            c.setCalle(m.getCalle());
            c.setNumero(m.getNumero());
            c.setProvincia(m.getProvincia());
            c.setDepartamentoGeografico(m.getDepartamentoGeografico());
            c.setLocalidad(m.getLocalidad());
            c.setAnioConstruccion(m.getAnioConstruccion());
            c.setAnioInicioInstitucion(m.getAnioInicioInstitucion());
            c.setPropiedadInstitucion(m.getPropiedadInstitucion());
            c.setCondicionTenencia(m.getCondicionTenencia());
            c.setSituacion(m.getSituacion());
            c.setVinculaTodasCarreras(m.getVinculaTodasCarreras());
            c.setEsMaestro(false);
            c.setProcesoId(procesoId);
            c.setSuperficieCubiertaM2(m.getSuperficieCubiertaM2());
            c.setSuperficieTotalM2(m.getSuperficieTotalM2());
            c.setObservaciones(m.getObservaciones());
            c.setVerificado(false);
            c.setEstadoConeau(m.getEstadoConeau());
            c.setActivo(true);
            c.setCreadoEn(Instant.now());
            c.setActualizadoEn(Instant.now());
            if (m.getCarreras() != null) {
                c.setCarreras(new HashSet<>(m.getCarreras()));
            }

            InmuebleJpaEntity guardado = inmuebleRepository.save(c);

            // Clonar espacios consultando directamente por inmuebleId
            List<EspacioFisicoJpaEntity> espaciosMaestro = espacioFisicoRepository.findByInmuebleIdAndActivoTrue(m.getId());
            for (EspacioFisicoJpaEntity espM : espaciosMaestro) {
                EspacioFisicoJpaEntity espC = new EspacioFisicoJpaEntity();
                espC.setId(UUID.randomUUID());
                espC.setInmueble(guardado);
                espC.setTipoEspacio(espM.getTipoEspacio());
                espC.setDenominacion(espM.getDenominacion());
                espC.setDescripcionInfraestructura(espM.getDescripcionInfraestructura());
                espC.setCantidad(espM.getCantidad());
                espC.setCapacidadPersonasSentadas(espM.getCapacidadPersonasSentadas());
                espC.setCapacidadEstudiantes(espM.getCapacidadEstudiantes());
                espC.setSuperficieM2(espM.getSuperficieM2());
                espC.setEquipamientoDisponible(espM.getEquipamientoDisponible());
                espC.setEstadoConeau(espM.getEstadoConeau());
                espC.setActivo(true);
                espC.setCreadoEn(Instant.now());
                espacioFisicoRepository.save(espC);
            }

            clones.add(guardado);
        }
        return clones;
    }

    // -------------------------------------------------------------------------
    // Catálogo Tipo de Espacio Físico
    // -------------------------------------------------------------------------

    @GetMapping("/tipos-espacio")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TipoEspacioResponse>> listarTiposEspacio() {
        List<TipoEspacioFisicoJpaEntity> tipos = tipoEspacioRepository.findByActivoTrueOrderByDenominacionAsc();
        List<TipoEspacioResponse> response = tipos.stream()
                .map(t -> new TipoEspacioResponse(t.getId(), t.getCodigo(), t.getDenominacion(), t.getActivo()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tipos-espacio")
    @Transactional
    public ResponseEntity<TipoEspacioResponse> crearTipoEspacio(@RequestBody CrearTipoEspacioRequest request) {
        String codigo = (request.codigo() != null && !request.codigo().isBlank())
                ? request.codigo().toUpperCase().replace(" ", "_")
                : request.denominacion().toUpperCase().replace(" ", "_");

        TipoEspacioFisicoJpaEntity entity = new TipoEspacioFisicoJpaEntity(UUID.randomUUID(), codigo, request.denominacion());
        TipoEspacioFisicoJpaEntity guardado = tipoEspacioRepository.save(entity);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TipoEspacioResponse(guardado.getId(), guardado.getCodigo(), guardado.getDenominacion(), guardado.getActivo()));
    }

    // -------------------------------------------------------------------------
    // Mappers y Helpers
    // -------------------------------------------------------------------------

    private InmuebleResponse mapToInmuebleResponse(InmuebleJpaEntity entity) {
        List<InmuebleResponse.CarreraVinculadaResponse> carreras = (entity.getCarreras() == null) ? Collections.emptyList() :
                entity.getCarreras().stream()
                        .map(c -> new InmuebleResponse.CarreraVinculadaResponse(c.getId(), c.getNombre(), c.getSigla()))
                        .collect(Collectors.toList());

        List<EspacioFisicoResponse> espacios = (entity.getEspacios() == null) ? Collections.emptyList() :
                entity.getEspacios().stream()
                        .filter(EspacioFisicoJpaEntity::getActivo)
                        .map(e -> new EspacioFisicoResponse(
                                e.getId(),
                                e.getTipoEspacio() != null ? e.getTipoEspacio().getId() : null,
                                e.getTipoEspacio() != null ? e.getTipoEspacio().getCodigo() : null,
                                e.getTipoEspacio() != null ? e.getTipoEspacio().getDenominacion() : null,
                                e.getDenominacion(),
                                e.getDescripcionInfraestructura(),
                                e.getCantidad() != null ? e.getCantidad() : 1,
                                e.getCapacidadPersonasSentadas() != null ? e.getCapacidadPersonasSentadas() : e.getCapacidadEstudiantes(),
                                e.getCapacidadEstudiantes(),
                                e.getSuperficieM2(),
                                e.getEquipamientoDisponible(),
                                e.getEstadoConeau(),
                                e.getActivo()
                        ))
                        .collect(Collectors.toList());

        List<DocumentacionRespaldoResponse> documentos = (entity.getDocumentos() == null) ? Collections.emptyList() :
                entity.getDocumentos().stream()
                        .map(d -> new DocumentacionRespaldoResponse(
                                d.getId(),
                                d.getNombreArchivo(),
                                d.getUrlMinio(),
                                d.getRutaRepositorio(),
                                d.getTipoAnexo(),
                                d.getDescripcion(),
                                d.getSubidoPorUsuario() != null ? d.getSubidoPorUsuario().getUsername() : null,
                                d.getFechaSubida()
                        ))
                        .collect(Collectors.toList());

        return new InmuebleResponse(
                entity.getId(),
                entity.getInstitucion() != null ? entity.getInstitucion().getId() : null,
                entity.getNumeroOrden(),
                entity.getNombre(),
                entity.getDomicilio(),
                entity.getCalle(),
                entity.getNumero(),
                entity.getProvincia() != null ? entity.getProvincia().getId() : null,
                entity.getProvincia() != null ? entity.getProvincia().getNombre() : null,
                entity.getDepartamentoGeografico() != null ? entity.getDepartamentoGeografico().getId() : null,
                entity.getDepartamentoGeografico() != null ? entity.getDepartamentoGeografico().getNombre() : null,
                entity.getLocalidad() != null ? entity.getLocalidad().getId() : null,
                entity.getLocalidad() != null ? entity.getLocalidad().getNombre() : null,
                entity.getAnioConstruccion(),
                entity.getAnioInicioInstitucion(),
                entity.getPropiedadInstitucion(),
                entity.getCondicionTenencia(),
                entity.getSituacion(),
                entity.getVinculaTodasCarreras(),
                entity.getEsMaestro(),
                entity.getProcesoId(),
                entity.getSuperficieCubiertaM2(),
                entity.getSuperficieTotalM2(),
                entity.getObservaciones(),
                entity.getVerificado(),
                entity.getFechaVerificacion(),
                entity.getEstadoConeau(),
                entity.getActivo(),
                carreras,
                espacios,
                documentos,
                espacios.size(),
                !documentos.isEmpty()
        );
    }

    private UUID resolverUsuarioAutenticadoId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            String principal = auth.getPrincipal().toString();
            Optional<User> userOpt = userRepository.findByEmail(principal);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            }
        }
        return userRepository.findByEmail("admin@undec.edu.ar")
                .map(User::getId)
                .orElse(UUID.fromString("b915837d-6a68-4ad1-95b9-5f6455e586ce"));
    }
}
