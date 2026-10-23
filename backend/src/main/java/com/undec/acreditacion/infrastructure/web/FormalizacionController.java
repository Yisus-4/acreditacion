package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.ConsultarPlanTrabajoUseCase;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.FichaFormalizacionFt012JpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataFichaFormalizacionFt012Repository;
import com.undec.acreditacion.infrastructure.web.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/convocatorias/{procesoId}", "/api/v1/procesos-acreditacion/{procesoId}"})
public class FormalizacionController {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final String CARPETA_NORMATIVA = "01-Convocatoria y formalizacion CONEAU/Normativa/";
    private static final String CARPETA_FORMALIZACION = "01-Convocatoria y formalizacion CONEAU/";

    private static final Set<String> CLASIFICACIONES_VALIDAS = Set.of(
            "DOC_PROPIA_AREA",
            "PROCESOS_ACREDITACION_CARRERAS",
            "EVALUACION_FUNCIONES",
            "EVALUACION_INSTITUCIONAL"
    );

    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final RegistroConvocatoriaRepositoryPort registroRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase;
    private final DocumentoAcreditacionRepositoryPort documentoRepository;
    private final DocumentStoragePort storagePort;
    private final SpringDataFichaFormalizacionFt012Repository fichaRepository;
    private final UserRepository userRepository;
    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private final SedeLocalizacionRepositoryPort sedeRepository;

    public FormalizacionController(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CarreraRepositoryPort carreraRepository,
            RegistroConvocatoriaRepositoryPort registroRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase,
            DocumentoAcreditacionRepositoryPort documentoRepository,
            DocumentStoragePort storagePort,
            SpringDataFichaFormalizacionFt012Repository fichaRepository,
            UserRepository userRepository,
            UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            SedeLocalizacionRepositoryPort sedeRepository) {
        this.procesoRepository = procesoRepository;
        this.carreraRepository = carreraRepository;
        this.registroRepository = registroRepository;
        this.planTrabajoRepository = planTrabajoRepository;
        this.consultarPlanTrabajoUseCase = consultarPlanTrabajoUseCase;
        this.documentoRepository = documentoRepository;
        this.storagePort = storagePort;
        this.fichaRepository = fichaRepository;
        this.userRepository = userRepository;
        this.unidadAcademicaRepository = unidadAcademicaRepository;
        this.sedeRepository = sedeRepository;
    }

    // =========================================================================
    // ACTIVIDAD 4: Identificar información y documentación de interés (Hoja INFO)
    // =========================================================================

    @GetMapping("/actividades/{actividadId}/documentos")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<List<DocumentoAcreditacionResponse>> listarDocumentosActividad(
            @PathVariable UUID procesoId,
            @PathVariable UUID actividadId
    ) {
        List<DocumentoAcreditacion> docs = documentoRepository.findByActividadId(actividadId);
        List<DocumentoAcreditacionResponse> response = docs.stream()
                .map(d -> mapearDocumentoResponse(procesoId, actividadId, d))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/actividades/{actividadId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('ACTIVIDAD_MANAGE')")
    @Transactional
    public ResponseEntity<DocumentoAcreditacionResponse> subirDocumentoInteres(
            @PathVariable UUID procesoId,
            @PathVariable UUID actividadId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("clasificacionOrigen") String clasificacionOrigen,
            @RequestParam(value = "descripcion", required = false) String descripcion
    ) throws IOException {
        validarPdf(archivo);
        String clasificacionNorm = clasificacionOrigen != null ? clasificacionOrigen.trim().toUpperCase() : "";
        if (!CLASIFICACIONES_VALIDAS.contains(clasificacionNorm)) {
            throw new DomainValidationException("Clasificación de origen inválida: " + clasificacionOrigen);
        }

        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "documento-interes.pdf";
        String objectPath = CARPETA_NORMATIVA + UUID.randomUUID() + "_" + sanitizarNombreArchivo(nombreOriginal);
        String storedPath = storagePort.uploadFile(archivo.getBytes(), objectPath, DocumentoAcreditacion.MIME_PDF);

        UUID usuarioId = resolverUsuarioAutenticadoId();
        DocumentoAcreditacion doc = DocumentoAcreditacion.crear(
                procesoId,
                actividadId,
                nombreOriginal,
                TipoDocumentoAcreditacion.DOCUMENTACION_INTERES,
                clasificacionNorm,
                descripcion,
                DocumentoAcreditacion.MIME_PDF,
                archivo.getSize(),
                storedPath,
                usuarioId
        );
        DocumentoAcreditacion saved = documentoRepository.save(doc);

        ActividadPlanTrabajo act = planTrabajoRepository.findActividadById(actividadId).orElse(null);
        if (act != null && act.getEstado() == EstadoActividad.PENDIENTE) {
            act.cambiarEstado(EstadoActividad.EN_PROGRESO);
            planTrabajoRepository.saveActividad(act);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapearDocumentoResponse(procesoId, actividadId, saved));
    }

    @DeleteMapping("/actividades/{actividadId}/documentos/{documentoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('ACTIVIDAD_MANAGE')")
    @Transactional
    public ResponseEntity<Void> eliminarDocumentoInteres(
            @PathVariable UUID procesoId,
            @PathVariable UUID actividadId,
            @PathVariable UUID documentoId
    ) {
        DocumentoAcreditacion doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el documento con ID: " + documentoId));
        try {
            storagePort.deleteFile(doc.getStoragePath());
        } catch (Exception ignored) {
        }
        documentoRepository.deleteById(documentoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/actividades/{actividadId}/documentos/{documentoId}/descargar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<byte[]> descargarDocumentoInteres(
            @PathVariable UUID procesoId,
            @PathVariable UUID actividadId,
            @PathVariable UUID documentoId
    ) {
        DocumentoAcreditacion doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el documento con ID: " + documentoId));
        byte[] bytes = storagePort.downloadFile(doc.getStoragePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNombreOriginal() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    @PostMapping("/actividades/{actividadId}/completar-documentacion-interes")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('ACTIVIDAD_MANAGE')")
    @Transactional
    public ResponseEntity<ActividadDetalleResponse> completarDocumentacionInteres(
            @PathVariable UUID procesoId,
            @PathVariable UUID actividadId,
            @RequestBody(required = false) CompletarDocumentacionInteresRequest body,
            @RequestParam(value = "fuentesSeleccionadas", required = false) List<String> fuentesParam,
            @RequestParam(value = "observaciones", required = false) String observacionesParam
    ) {
        ActividadPlanTrabajo actividad = obtenerActividadAsegurada(procesoId, actividadId, ActividadPlanTrabajo.CODIGO_ACT_04);

        List<String> fuentesRaw = (body != null && body.fuentesSeleccionadas() != null && !body.fuentesSeleccionadas().isEmpty())
                ? body.fuentesSeleccionadas()
                : fuentesParam;
        String observaciones = (body != null && body.observaciones() != null)
                ? body.observaciones()
                : observacionesParam;

        List<DocumentacionRemitidaProceso> fuentes = parsearCodigosDocumentacion(fuentesRaw);
        if (fuentes.isEmpty()) {
            fuentes = List.of(
                    DocumentacionRemitidaProceso.DOC_PROPIA_AREA,
                    DocumentacionRemitidaProceso.PROCESOS_ACREDITACION_CARRERAS,
                    DocumentacionRemitidaProceso.EVALUACION_FUNCIONES,
                    DocumentacionRemitidaProceso.EVALUACION_INSTITUCIONAL
            );
        }

        UUID usuarioId = resolverUsuarioAutenticadoId();
        actividad.completarDocumentacionInteres(fuentes, observaciones, usuarioId);
        ActividadPlanTrabajo saved = planTrabajoRepository.saveActividad(actividad);

        return ResponseEntity.ok(mapearActividadResponse(saved, usuarioId));
    }

    // =========================================================================
    // ACTIVIDAD 5: Formalización electrónica de participación en convocatoria (FT-012 + RG-001)
    // =========================================================================

    @GetMapping("/formalizacion")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<FichaFormalizacionFt012Response> obtenerFormalizacion(@PathVariable UUID procesoId) {
        Optional<FichaFormalizacionFt012JpaEntity> fichaOpt = fichaRepository.findByProcesoId(procesoId);
        if (fichaOpt.isPresent()) {
            return ResponseEntity.ok(mapearFichaResponse(fichaOpt.get()));
        }
        FichaFormalizacionFt012JpaEntity inicial = construirFichaInicialConDefaultsUndec(procesoId);
        return ResponseEntity.ok(mapearFichaResponse(inicial));
    }

    @PostMapping("/formalizacion/solicitar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('ACTIVIDAD_MANAGE')")
    @Transactional
    public ResponseEntity<FichaFormalizacionFt012Response> solicitarFichaFormalizacion(@PathVariable UUID procesoId) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        FichaFormalizacionFt012JpaEntity entity = fichaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> construirFichaInicialConDefaultsUndec(procesoId));

        if (!"FORMALIZADA_EN_CONEAU".equals(entity.getEstado())) {
            entity.setEstado("SOLICITADA_A_DC");
        }
        entity.setSolicitadoPor(usuarioId);
        entity.setSolicitadoEn(Instant.now());
        entity.setUpdatedAt(Instant.now());
        FichaFormalizacionFt012JpaEntity saved = fichaRepository.save(entity);

        PlanTrabajo plan = consultarPlanTrabajoUseCase.obtenerPorProcesoId(procesoId);
        plan.getActividades().stream()
                .filter(PlanTrabajo::esActividadFormalizacionElectronica)
                .findFirst()
                .ifPresent(act6 -> {
                    if (act6.getEstado() == EstadoActividad.PENDIENTE) {
                        act6.cambiarEstado(EstadoActividad.EN_PROGRESO);
                        planTrabajoRepository.saveActividad(act6);
                    }
                });

        return ResponseEntity.ok(mapearFichaResponse(saved));
    }

    @PutMapping("/formalizacion/ficha")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    @Transactional
    public ResponseEntity<FichaFormalizacionFt012Response> guardarFichaFormalizacion(
            @PathVariable UUID procesoId,
            @RequestBody GuardarFichaFormalizacionFt012Request request
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        FichaFormalizacionFt012JpaEntity entity = fichaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> construirFichaInicialConDefaultsUndec(procesoId));

        aplicarCambiosFicha(entity, request);

        boolean enviar = Boolean.TRUE.equals(request.enviar());
        if (enviar) {
            entity.setEstado("REMITIDA_POR_DC");
            entity.setRemitidoPor(usuarioId);
            entity.setRemitidoEn(Instant.now());
        } else if (!"FORMALIZADA_EN_CONEAU".equals(entity.getEstado())) {
            entity.setEstado("BORRADOR");
        }
        entity.setUpdatedAt(Instant.now());

        FichaFormalizacionFt012JpaEntity saved = fichaRepository.save(entity);

        PlanTrabajo plan = consultarPlanTrabajoUseCase.obtenerPorProcesoId(procesoId);
        plan.getActividades().stream()
                .filter(PlanTrabajo::esActividadFormalizacionElectronica)
                .findFirst()
                .ifPresent(act6 -> {
                    if (act6.getEstado() == EstadoActividad.PENDIENTE) {
                        act6.cambiarEstado(EstadoActividad.EN_PROGRESO);
                        planTrabajoRepository.saveActividad(act6);
                    }
                });

        return ResponseEntity.ok(mapearFichaResponse(saved));
    }

    @PostMapping(value = "/formalizacion/formalizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('ACTIVIDAD_MANAGE')")
    @Transactional
    public ResponseEntity<FichaFormalizacionFt012Response> formalizarEnConeau(
            @PathVariable UUID procesoId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("fechaFormalizacionElectronica") String fechaFormalizacionStr,
            @RequestParam(value = "observaciones", required = false) String observaciones
    ) throws IOException {
        validarPdf(archivo);
        LocalDate fechaFormalizacion = parsearFechaLocal(
                fechaFormalizacionStr,
                "La fecha de formalización electrónica es obligatoria"
        );

        UUID usuarioId = resolverUsuarioAutenticadoId();
        String nombreOriginal = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename()
                : "Comprobante_Formalizacion_CONEAU_Global.pdf";
        String objectPath = CARPETA_FORMALIZACION + UUID.randomUUID() + "_" + sanitizarNombreArchivo(nombreOriginal);
        String storedPath = storagePort.uploadFile(archivo.getBytes(), objectPath, DocumentoAcreditacion.MIME_PDF);

        FichaFormalizacionFt012JpaEntity entity = fichaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> construirFichaInicialConDefaultsUndec(procesoId));

        entity.setEstado("FORMALIZADA_EN_CONEAU");
        entity.setComprobanteNombre(nombreOriginal);
        entity.setComprobanteStoragePath(storedPath);
        entity.setComprobanteTamanioBytes(archivo.getSize());
        entity.setFechaFormalizacionElectronica(fechaFormalizacion);
        if (observaciones != null && !observaciones.isBlank()) {
            entity.setObservaciones(observaciones.trim());
        }
        entity.setFormalizadoPor(usuarioId);
        entity.setFormalizadoEn(Instant.now());
        entity.setUpdatedAt(Instant.now());
        FichaFormalizacionFt012JpaEntity savedFicha = fichaRepository.save(entity);

        // Actualizar RegistroConvocatoria (RG-001) con la fecha de formalización electrónica
        registroRepository.findByProcesoId(procesoId).ifPresent(reg -> {
            reg.registrarFechaFormalizacionElectronica(fechaFormalizacion);
            registroRepository.save(reg);
        });

        // Marcar Actividad 6 (ACT-06-FORMALIZACION-ELECTRONICA) como COMPLETADA en hoja CONEAU
        PlanTrabajo plan = consultarPlanTrabajoUseCase.obtenerPorProcesoId(procesoId);
        ActividadPlanTrabajo act6 = plan.getActividades().stream()
                .filter(PlanTrabajo::esActividadFormalizacionElectronica)
                .findFirst()
                .orElseGet(() -> {
                    ActividadPlanTrabajo nueva = ActividadPlanTrabajo.crearActividadFormalizacionElectronica(plan.getId());
                    plan.agregarActividad(nueva);
                    return planTrabajoRepository.saveActividad(nueva);
                });

        DocumentoAcreditacion docComprobante = DocumentoAcreditacion.crear(
                procesoId,
                act6.getId(),
                nombreOriginal,
                TipoDocumentoAcreditacion.COMPROBANTE_FORMALIZACION_CONEAU,
                "CONEAU_GLOBAL_ATENEA",
                "Comprobante de formalización electrónica generado desde CONEAU Global ATENEA",
                DocumentoAcreditacion.MIME_PDF,
                archivo.getSize(),
                storedPath,
                usuarioId
        );
        documentoRepository.save(docComprobante);

        Instant fechaInstant = fechaFormalizacion.atStartOfDay(ZONA_ARGENTINA).toInstant();
        act6.completarFormalizacionElectronica(fechaInstant, observaciones, usuarioId);
        planTrabajoRepository.saveActividad(act6);

        return ResponseEntity.ok(mapearFichaResponse(savedFicha));
    }

    @GetMapping("/formalizacion/comprobante")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<byte[]> descargarComprobanteFormalizacion(@PathVariable UUID procesoId) {
        FichaFormalizacionFt012JpaEntity ficha = fichaRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la ficha de formalización FT-012 para el proceso: " + procesoId));

        if (ficha.getComprobanteStoragePath() == null || ficha.getComprobanteStoragePath().isBlank()) {
            throw new ResourceNotFoundException("Aún no se ha adjuntado el comprobante PDF de formalización electrónica");
        }

        byte[] bytes = storagePort.downloadFile(ficha.getComprobanteStoragePath());
        String filename = ficha.getComprobanteNombre() != null
                ? ficha.getComprobanteNombre()
                : "Comprobante_Formalizacion_CONEAU.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ActividadPlanTrabajo obtenerActividadAsegurada(UUID procesoId, UUID actividadId, String codigoFallback) {
        Optional<ActividadPlanTrabajo> actOpt = planTrabajoRepository.findActividadById(actividadId);
        if (actOpt.isPresent()) {
            return actOpt.get();
        }
        PlanTrabajo plan = consultarPlanTrabajoUseCase.obtenerPorProcesoId(procesoId);
        return plan.getActividades().stream()
                .filter(a -> a.getId().equals(actividadId) || codigoFallback.equals(a.getCodigoActividad()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la actividad con ID: " + actividadId));
    }

    private FichaFormalizacionFt012JpaEntity construirFichaInicialConDefaultsUndec(UUID procesoId) {
        ProcesoAcreditacion proceso = procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));
        Optional<Carrera> carreraOpt = carreraRepository.findById(proceso.getCarreraId());
        Optional<RegistroConvocatoria> regOpt = registroRepository.findByProcesoId(procesoId);

        String modalidad = regOpt.map(RegistroConvocatoria::getModalidadCarrera)
                .orElseGet(() -> carreraOpt.map(c -> c.getModalidad() == ModalidadCarrera.DISTANCIA ? "A_DISTANCIA" : "PRESENCIAL")
                        .orElse("PRESENCIAL"));

        String unidadAcademicaNombre = "Universidad Nacional de Chilecito";
        String calle = "9 de Julio";
        String numero = "22";
        String cp = "5360";
        String provincia = "La Rioja";
        String localidad = "Chilecito";
        String departamentoPartido = "Chilecito";

        if (carreraOpt.isPresent()) {
            Carrera c = carreraOpt.get();
            if (c.getUnidadAcademicaId() != null) {
                unidadAcademicaNombre = unidadAcademicaRepository.findById(c.getUnidadAcademicaId())
                        .map(UnidadAcademica::getNombre)
                        .filter(nom -> !nom.isBlank())
                        .orElse(unidadAcademicaNombre);
            }
            if (c.getSedesIds() != null && !c.getSedesIds().isEmpty()) {
                UUID sedeId = c.getSedesIds().get(0);
                Optional<SedeLocalizacion> sOpt = sedeRepository.findById(sedeId);
                if (sOpt.isPresent()) {
                    SedeLocalizacion s = sOpt.get();
                    if (s.getCalle() != null && !s.getCalle().isBlank()) calle = s.getCalle();
                    if (s.getNumero() != null && !s.getNumero().isBlank()) numero = s.getNumero();
                    if (s.getCodigoPostal() != null && !s.getCodigoPostal().isBlank()) cp = s.getCodigoPostal();
                }
            }
        }

        FichaFormalizacionFt012JpaEntity entity = new FichaFormalizacionFt012JpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setProcesoId(procesoId);
        entity.setModalidadDictado(modalidad);
        entity.setEstado("PENDIENTE_SOLICITUD");
        entity.setInstitucionUniversitaria("Universidad Nacional de Chilecito");
        entity.setAutoridadApellido("Salcedo");
        entity.setAutoridadPrimerNombre("César");
        entity.setAutoridadSegundoNombre("Alberto");
        entity.setAutoridadCorreo("csalcedo@undec.edu.ar");
        entity.setAutoridadTelefono("");
        entity.setUnidadAcademica(unidadAcademicaNombre);
        entity.setSedeCalle(calle);
        entity.setSedeNumero(numero);
        entity.setSedeCodigoPostal(cp);
        entity.setSedeProvincia(provincia);
        entity.setSedeLocalidad(localidad);
        entity.setSedeDepartamentoPartido(departamentoPartido);
        entity.setCarreraNombre(carreraOpt.map(Carrera::getNombre).orElse(""));
        entity.setTituloAsimilado(carreraOpt.map(Carrera::getTituloOtorgado).orElse(""));
        entity.setResponsableCargo("");
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private void aplicarCambiosFicha(FichaFormalizacionFt012JpaEntity entity, GuardarFichaFormalizacionFt012Request req) {
        if (req == null) return;
        if (req.modalidadDictado() != null && !req.modalidadDictado().isBlank()) {
            entity.setModalidadDictado(req.modalidadDictado().trim());
        }
        if (req.institucionUniversitaria() != null && !req.institucionUniversitaria().isBlank()) {
            entity.setInstitucionUniversitaria(req.institucionUniversitaria().trim());
        }
        if (req.autoridadApellido() != null && !req.autoridadApellido().isBlank()) {
            entity.setAutoridadApellido(req.autoridadApellido().trim());
        }
        if (req.autoridadPrimerNombre() != null && !req.autoridadPrimerNombre().isBlank()) {
            entity.setAutoridadPrimerNombre(req.autoridadPrimerNombre().trim());
        }
        entity.setAutoridadSegundoNombre(trimOrNull(req.autoridadSegundoNombre()));
        if (req.autoridadCorreo() != null && !req.autoridadCorreo().isBlank()) {
            entity.setAutoridadCorreo(req.autoridadCorreo().trim());
        }
        entity.setAutoridadTelefono(trimOrNull(req.autoridadTelefono()));
        if (req.unidadAcademica() != null && !req.unidadAcademica().isBlank()) {
            entity.setUnidadAcademica(req.unidadAcademica().trim());
        }
        if (req.sedeCalle() != null && !req.sedeCalle().isBlank()) {
            entity.setSedeCalle(req.sedeCalle().trim());
        }
        if (req.sedeNumero() != null && !req.sedeNumero().isBlank()) {
            entity.setSedeNumero(req.sedeNumero().trim());
        }
        entity.setSedePiso(trimOrNull(req.sedePiso()));
        entity.setSedeDepartamento(trimOrNull(req.sedeDepartamento()));
        if (req.sedeCodigoPostal() != null && !req.sedeCodigoPostal().isBlank()) {
            entity.setSedeCodigoPostal(req.sedeCodigoPostal().trim());
        }
        if (req.sedeProvincia() != null && !req.sedeProvincia().isBlank()) {
            entity.setSedeProvincia(req.sedeProvincia().trim());
        }
        if (req.sedeLocalidad() != null && !req.sedeLocalidad().isBlank()) {
            entity.setSedeLocalidad(req.sedeLocalidad().trim());
        }
        if (req.sedeDepartamentoPartido() != null && !req.sedeDepartamentoPartido().isBlank()) {
            entity.setSedeDepartamentoPartido(req.sedeDepartamentoPartido().trim());
        }
        entity.setNexoApellido(trimOrNull(req.nexoApellido()));
        entity.setNexoPrimerNombre(trimOrNull(req.nexoPrimerNombre()));
        entity.setNexoSegundoNombre(trimOrNull(req.nexoSegundoNombre()));
        entity.setNexoTelefono(trimOrNull(req.nexoTelefono()));
        entity.setNexoCargo(trimOrNull(req.nexoCargo()));
        entity.setNexoCorreo(trimOrNull(req.nexoCorreo()));
        if (req.carreraNombre() != null) {
            entity.setCarreraNombre(req.carreraNombre().trim());
        }
        entity.setTituloAsimilado(trimOrNull(req.tituloAsimilado()));
        entity.setCargaHorariaNoPresencial(trimOrNull(req.cargaHorariaNoPresencial()));
        entity.setAnioInicioDictado(req.anioInicioDictado());
        entity.setResponsableApellido(trimOrNull(req.responsableApellido()));
        entity.setResponsablePrimerNombre(trimOrNull(req.responsablePrimerNombre()));
        entity.setResponsableSegundoNombre(trimOrNull(req.responsableSegundoNombre()));
        entity.setResponsableTelefono(trimOrNull(req.responsableTelefono()));
        entity.setResponsableCorreo(trimOrNull(req.responsableCorreo()));
        if (req.responsableCargo() != null && !req.responsableCargo().isBlank()) {
            entity.setResponsableCargo(req.responsableCargo().trim());
        }
        entity.setCoordinadorCaApellido(trimOrNull(req.coordinadorCaApellido()));
        entity.setCoordinadorCaPrimerNombre(trimOrNull(req.coordinadorCaPrimerNombre()));
        entity.setCoordinadorCaSegundoNombre(trimOrNull(req.coordinadorCaSegundoNombre()));
        entity.setCoordinadorCaTelefono(trimOrNull(req.coordinadorCaTelefono()));
        entity.setCoordinadorCaCorreo(trimOrNull(req.coordinadorCaCorreo()));
        if (req.observaciones() != null) {
            entity.setObservaciones(trimOrNull(req.observaciones()));
        }
    }

    private FichaFormalizacionFt012Response mapearFichaResponse(FichaFormalizacionFt012JpaEntity e) {
        return new FichaFormalizacionFt012Response(
                e.getId(),
                e.getProcesoId(),
                e.getModalidadDictado(),
                e.getEstado(),
                e.getInstitucionUniversitaria(),
                e.getAutoridadApellido(),
                e.getAutoridadPrimerNombre(),
                e.getAutoridadSegundoNombre(),
                e.getAutoridadCorreo(),
                e.getAutoridadTelefono(),
                e.getUnidadAcademica(),
                e.getSedeCalle(),
                e.getSedeNumero(),
                e.getSedePiso(),
                e.getSedeDepartamento(),
                e.getSedeCodigoPostal(),
                e.getSedeProvincia(),
                e.getSedeLocalidad(),
                e.getSedeDepartamentoPartido(),
                e.getNexoApellido(),
                e.getNexoPrimerNombre(),
                e.getNexoSegundoNombre(),
                e.getNexoTelefono(),
                e.getNexoCargo(),
                e.getNexoCorreo(),
                e.getCarreraNombre(),
                e.getTituloAsimilado(),
                e.getCargaHorariaNoPresencial(),
                e.getAnioInicioDictado(),
                e.getResponsableApellido(),
                e.getResponsablePrimerNombre(),
                e.getResponsableSegundoNombre(),
                e.getResponsableTelefono(),
                e.getResponsableCorreo(),
                e.getResponsableCargo(),
                e.getCoordinadorCaApellido(),
                e.getCoordinadorCaPrimerNombre(),
                e.getCoordinadorCaSegundoNombre(),
                e.getCoordinadorCaTelefono(),
                e.getCoordinadorCaCorreo(),
                e.getComprobanteNombre(),
                e.getComprobanteStoragePath(),
                e.getComprobanteTamanioBytes(),
                e.getFechaFormalizacionElectronica(),
                e.getObservaciones(),
                e.getSolicitadoPor(),
                e.getSolicitadoEn(),
                e.getRemitidoPor(),
                e.getRemitidoEn(),
                e.getFormalizadoPor(),
                e.getFormalizadoEn(),
                e.getUpdatedAt()
        );
    }

    private DocumentoAcreditacionResponse mapearDocumentoResponse(UUID procesoId, UUID actividadId, DocumentoAcreditacion d) {
        return new DocumentoAcreditacionResponse(
                d.getId(),
                d.getNombreOriginal(),
                d.getTipoDocumento().name(),
                d.getClasificacionOrigen(),
                d.getDescripcion(),
                d.getMimeType(),
                d.getTamanioBytes(),
                "/api/convocatorias/" + procesoId + "/actividades/" + actividadId + "/documentos/" + d.getId() + "/descargar",
                d.getCreadoEn()
        );
    }

    private ActividadDetalleResponse mapearActividadResponse(ActividadPlanTrabajo act, UUID usuarioId) {
        List<DocumentoAcreditacion> docs = documentoRepository.findByActividadId(act.getId());
        List<DocumentoAcreditacionResponse> docsDto = docs.stream()
                .map(d -> new DocumentoAcreditacionResponse(
                        d.getId(),
                        d.getNombreOriginal(),
                        d.getTipoDocumento().name(),
                        d.getClasificacionOrigen(),
                        d.getDescripcion(),
                        d.getMimeType(),
                        d.getTamanioBytes(),
                        "/api/documentos/" + d.getId() + "/descargar",
                        d.getCreadoEn()
                ))
                .collect(Collectors.toList());

        String nombreUsuario = userRepository.findById(usuarioId)
                .map(User::getUsername)
                .orElse("Usuario AEA");

        return new ActividadDetalleResponse(
                act.getId(),
                act.getPlanTrabajoId(),
                act.getNumeroOrden(),
                act.getCodigoActividad(),
                act.getNombre(),
                act.getEstado().name(),
                act.getFechaReunionCoordinacion(),
                act.getSintesisAcuerdos(),
                act.getCompletadaEn(),
                act.getCompletadaPor(),
                nombreUsuario,
                act.getHoja() != null ? act.getHoja().name() : "INFO",
                act.getFechaInicioEstimada(),
                act.getFechaFinEstimada(),
                act.getResponsable(),
                act.getPrioridad() != null ? act.getPrioridad().name() : "ALTA",
                act.getCreadoEn(),
                act.getActualizadoEn(),
                docsDto,
                act.getAreasInformadas().stream().map(Enum::name).toList(),
                act.getDocumentacionRemitida().stream().map(Enum::name).toList(),
                act.getObservaciones()
        );
    }

    private static List<DocumentacionRemitidaProceso> parsearCodigosDocumentacion(List<String> valores) {
        if (valores == null || valores.isEmpty()) {
            return List.of();
        }
        List<DocumentacionRemitidaProceso> resultado = new ArrayList<>();
        for (String valor : valores) {
            if (valor == null || valor.isBlank()) continue;
            for (String parte : valor.split(",")) {
                String codigo = parte.trim().toUpperCase();
                if (codigo.isEmpty()) continue;
                try {
                    resultado.add(DocumentacionRemitidaProceso.valueOf(codigo));
                } catch (IllegalArgumentException ex) {
                    throw new DomainValidationException("Código inválido: " + codigo);
                }
            }
        }
        return resultado;
    }

    private static LocalDate parsearFechaLocal(String fechaStr, String mensajeObligatorio) {
        if (fechaStr == null || fechaStr.isBlank()) {
            throw new DomainValidationException(mensajeObligatorio);
        }
        String valor = fechaStr.trim();
        if (valor.contains("T")) {
            valor = valor.substring(0, valor.indexOf('T'));
        }
        List<DateTimeFormatter> formatos = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );
        for (DateTimeFormatter fmt : formatos) {
            try {
                return LocalDate.parse(valor, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new DomainValidationException("Formato de fecha inválido: " + fechaStr);
    }

    private static void validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new DomainValidationException("El archivo PDF es obligatorio");
        }
        String mimeType = archivo.getContentType();
        String filename = archivo.getOriginalFilename();
        boolean esPdf = (mimeType != null && DocumentoAcreditacion.MIME_PDF.equalsIgnoreCase(mimeType.trim()))
                || (filename != null && filename.toLowerCase().endsWith(".pdf"));
        if (!esPdf) {
            throw new DomainValidationException("Solo se admiten documentos en formato PDF");
        }
        if (archivo.getSize() > DocumentoAcreditacion.MAX_TAMANIO_BYTES) {
            throw new DomainValidationException("El archivo excede el tamaño máximo permitido de 20 MB");
        }
    }

    private static String sanitizarNombreArchivo(String nombre) {
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String trimOrNull(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
                .orElseGet(() -> userRepository.findAll().stream().findFirst().map(User::getId)
                        .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }
}
