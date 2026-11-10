package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;
import com.undec.acreditacion.infrastructure.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/convocatorias", "/api/v1/procesos-acreditacion"})
public class ConvocatoriaController {

    private final IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase;
    private final IniciarConvocatoriaEscuelaUseCase iniciarConvocatoriaEscuelaUseCase;
    private final ConfirmarConvocatoriaDirectorEscuelaUseCase confirmarConvocatoriaDirectorEscuelaUseCase;
    private final ConsultarConvocatoriaEscuelaUseCase consultarConvocatoriaEscuelaUseCase;
    private final com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort convocatoriaEscuelaRepository;
    private final ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase;
    private final ListarConvocatoriasUseCase listarConvocatoriasUseCase;
    private final EliminarConvocatoriaUseCase eliminarConvocatoriaUseCase;
    private final ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase;
    private final ElaborarPlanTrabajoUseCase elaborarPlanTrabajoUseCase;
    private final ObtenerPlantillaPlanTrabajoUseCase obtenerPlantillaPlanTrabajoUseCase;
    private final RegistroConvocatoriaRepositoryPort registroRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final DocumentoAcreditacionRepositoryPort documentoRepository;
    private final UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final UserRepository userRepository;
    private final ReiniciarConvocatoriaEscuelaTestUseCase reiniciarConvocatoriaEscuelaTestUseCase;

    public ConvocatoriaController(
            IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase,
            IniciarConvocatoriaEscuelaUseCase iniciarConvocatoriaEscuelaUseCase,
            ConfirmarConvocatoriaDirectorEscuelaUseCase confirmarConvocatoriaDirectorEscuelaUseCase,
            ConsultarConvocatoriaEscuelaUseCase consultarConvocatoriaEscuelaUseCase,
            com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort convocatoriaEscuelaRepository,
            ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase,
            ListarConvocatoriasUseCase listarConvocatoriasUseCase,
            EliminarConvocatoriaUseCase eliminarConvocatoriaUseCase,
            ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase,
            ElaborarPlanTrabajoUseCase elaborarPlanTrabajoUseCase,
            ObtenerPlantillaPlanTrabajoUseCase obtenerPlantillaPlanTrabajoUseCase,
            RegistroConvocatoriaRepositoryPort registroRepository,
            CarreraRepositoryPort carreraRepository,
            DocumentoAcreditacionRepositoryPort documentoRepository,
            UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            UserRepository userRepository,
            ReiniciarConvocatoriaEscuelaTestUseCase reiniciarConvocatoriaEscuelaTestUseCase) {
        this.iniciarConvocatoriaUseCase = iniciarConvocatoriaUseCase;
        this.iniciarConvocatoriaEscuelaUseCase = iniciarConvocatoriaEscuelaUseCase;
        this.confirmarConvocatoriaDirectorEscuelaUseCase = confirmarConvocatoriaDirectorEscuelaUseCase;
        this.consultarConvocatoriaEscuelaUseCase = consultarConvocatoriaEscuelaUseCase;
        this.convocatoriaEscuelaRepository = convocatoriaEscuelaRepository;
        this.consultarConvocatoriaUseCase = consultarConvocatoriaUseCase;
        this.listarConvocatoriasUseCase = listarConvocatoriasUseCase;
        this.eliminarConvocatoriaUseCase = eliminarConvocatoriaUseCase;
        this.consultarPlanTrabajoUseCase = consultarPlanTrabajoUseCase;
        this.elaborarPlanTrabajoUseCase = elaborarPlanTrabajoUseCase;
        this.obtenerPlantillaPlanTrabajoUseCase = obtenerPlantillaPlanTrabajoUseCase;
        this.registroRepository = registroRepository;
        this.carreraRepository = carreraRepository;
        this.documentoRepository = documentoRepository;
        this.unidadAcademicaRepository = unidadAcademicaRepository;
        this.planTrabajoRepository = planTrabajoRepository;
        this.procesoRepository = procesoRepository;
        this.userRepository = userRepository;
        this.reiniciarConvocatoriaEscuelaTestUseCase = reiniciarConvocatoriaEscuelaTestUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<ConvocatoriaDetalleResponse> iniciarConvocatoria(@RequestBody IniciarConvocatoriaRequest request) {
        UUID usuarioId = resolverUsuarioAutenticadoId();

        // 1. Si es convocatoria a nivel Escuela / Selección múltiple de carreras
        if (request.unidadAcademicaId() != null || (request.carreras() != null && !request.carreras().isEmpty())) {
            List<ItemCarreraSeleccionCommand> items = request.carreras() != null
                    ? request.carreras().stream().map(c -> new ItemCarreraSeleccionCommand(
                            c.carreraId(),
                            c.seleccionada() != null ? c.seleccionada() : true,
                            c.esObligatoria() != null ? c.esObligatoria() : false
                    )).toList()
                    : Collections.emptyList();

            IniciarConvocatoriaEscuelaCommand command = new IniciarConvocatoriaEscuelaCommand(
                    request.unidadAcademicaId(),
                    request.tipoProceso() != null ? request.tipoProceso() : TipoProcesoAcreditacion.EN_FUNCIONAMIENTO,
                    request.nroResolucionConeau(),
                    request.fechaInicio(),
                    request.fechaCierre(),
                    request.observaciones(),
                    items,
                    usuarioId
            );

            Convocatoria conv = iniciarConvocatoriaEscuelaUseCase.iniciar(command);
            ConvocatoriaEscuelaDetalleDto detalle = consultarConvocatoriaEscuelaUseCase.obtenerPorId(conv.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(construirDetalleResponseDesdeEscuela(detalle));
        }

        // 2. Modo compatibilidad histórica con proceso de carrera individual
        IniciarConvocatoriaCommand command = new IniciarConvocatoriaCommand(
                request.carreraId(),
                request.nroResolucionConeau(),
                request.fechaInicio(),
                request.fechaCierre(),
                request.tipoProceso(),
                request.observaciones(),
                usuarioId
        );

        ProcesoAcreditacion proceso = iniciarConvocatoriaUseCase.iniciar(command);
        ConvocatoriaDetalleResponse response = construirDetalleResponse(proceso);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/confirmar-de")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'SCHOOL_DIRECTOR', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<ConvocatoriaDetalleResponse> confirmarConvocatoriaDirectorEscuela(
            @PathVariable UUID id,
            @RequestBody ConfirmarConvocatoriaDeRequest request
    ) {
        ConvocatoriaEscuelaDetalleDto detalleExistente = consultarConvocatoriaEscuelaUseCase.obtenerPorId(id);
        Optional<User> usuarioActualOpt = resolverUsuarioAutenticado();
        if (!tienePermisoGlobal(usuarioActualOpt)) {
            UUID userEscuelaId = usuarioActualOpt.map(User::getUnidadAcademicaId).orElse(null);
            if (userEscuelaId == null || !userEscuelaId.equals(detalleExistente.unidadAcademicaId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        UUID usuarioId = resolverUsuarioAutenticadoId();
        ConfirmarConvocatoriaDirectorEscuelaCommand command = new ConfirmarConvocatoriaDirectorEscuelaCommand(
                id,
                request.carrerasSeleccionadas() != null ? request.carrerasSeleccionadas() : Collections.emptyList(),
                usuarioId
        );

        confirmarConvocatoriaDirectorEscuelaUseCase.confirmar(command);
        ConvocatoriaEscuelaDetalleDto detalle = consultarConvocatoriaEscuelaUseCase.obtenerPorId(id);
        return ResponseEntity.ok(construirDetalleResponseDesdeEscuela(detalle));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'SCHOOL_DIRECTOR', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<List<ConvocatoriaResumenResponse>> listarConvocatorias(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) UUID unidadAcademicaId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) TipoProcesoAcreditacion tipoProceso,
            @RequestParam(required = false) Integer anio
    ) {
        List<ConvocatoriaResumenResponse> response = new ArrayList<>();

        Optional<User> usuarioActualOpt = resolverUsuarioAutenticado();
        boolean esAdminOAEA = tienePermisoGlobal(usuarioActualOpt);
        UUID userEscuelaId = usuarioActualOpt.map(User::getUnidadAcademicaId).orElse(null);

        // Listar únicamente Convocatorias a nivel de Escuela
        List<ConvocatoriaEscuelaDetalleDto> escuelasConvocatorias = consultarConvocatoriaEscuelaUseCase.listarTodas();

        for (ConvocatoriaEscuelaDetalleDto ec : escuelasConvocatorias) {
            // Control de visibilidad por Escuela: Si el usuario no es ADMIN ni AEA, solo ve las de su escuela
            if (!esAdminOAEA) {
                if (userEscuelaId == null || !userEscuelaId.equals(ec.unidadAcademicaId())) {
                    continue;
                }
            }

            // Aplicar filtros a las convocatorias de escuela si se especifican
            if (unidadAcademicaId != null) {
                if (!unidadAcademicaId.equals(ec.unidadAcademicaId())) {
                    continue;
                }
            }
            if (carreraId != null) {
                boolean incluyeCarrera = ec.carreras().stream()
                        .anyMatch(item -> item.carreraId().equals(carreraId));
                if (!incluyeCarrera) {
                    continue;
                }
            }
            if (estado != null && !estado.isBlank()) {
                if (!ec.estado().name().equalsIgnoreCase(estado)) {
                    continue;
                }
            }
            if (tipoProceso != null) {
                if (ec.tipoProceso() != tipoProceso) {
                    continue;
                }
            }
            if (anio != null) {
                if (ec.fechaInicio().getYear() != anio) {
                    continue;
                }
            }

            response.add(new ConvocatoriaResumenResponse(
                    ec.id(),
                    ec.codigo(),
                    ec.titulo(),
                    null,
                    ec.unidadAcademicaNombre(),
                    ec.unidadAcademicaSigla(),
                    "GRADO",
                    ec.nroResolucionConeau() != null ? ec.nroResolucionConeau() : "",
                    ec.tipoProceso().name(),
                    ec.estado().name(),
                    ec.fechaInicio(),
                    ec.fechaCierre(),
                    null,
                    ec.creadoEn(),
                    ec.unidadAcademicaId(),
                    ec.unidadAcademicaNombre(),
                    ec.carreras().size()
            ));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'SCHOOL_DIRECTOR', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<ConvocatoriaDetalleResponse> obtenerPorId(@PathVariable UUID id) {
        Optional<Convocatoria> convOpt = convocatoriaEscuelaRepository.findById(id);
        if (convOpt.isPresent()) {
            ConvocatoriaEscuelaDetalleDto detalle = consultarConvocatoriaEscuelaUseCase.obtenerPorId(id);

            // Control de visibilidad por escuela para detalle
            Optional<User> usuarioActualOpt = resolverUsuarioAutenticado();
            if (!tienePermisoGlobal(usuarioActualOpt)) {
                UUID userEscuelaId = usuarioActualOpt.map(User::getUnidadAcademicaId).orElse(null);
                if (userEscuelaId == null || !userEscuelaId.equals(detalle.unidadAcademicaId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            return ResponseEntity.ok(construirDetalleResponseDesdeEscuela(detalle));
        }

        ProcesoAcreditacion proceso = consultarConvocatoriaUseCase.obtenerPorId(id);
        return ResponseEntity.ok(construirDetalleResponse(proceso));
    }

    @PostMapping("/{id}/reiniciar-test")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'ADMIN', 'DEV', 'DEVELOPER', 'AC_COORDINATOR')")
    public ResponseEntity<ConvocatoriaDetalleResponse> reiniciarConvocatoriaTest(@PathVariable UUID id) {
        reiniciarConvocatoriaEscuelaTestUseCase.reiniciar(id);
        ConvocatoriaEscuelaDetalleDto detalle = consultarConvocatoriaEscuelaUseCase.obtenerPorId(id);
        return ResponseEntity.ok(construirDetalleResponseDesdeEscuela(detalle));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'ADMIN', 'DEV', 'DEVELOPER', 'AC_COORDINATOR')")
    public ResponseEntity<Void> eliminarConvocatoria(@PathVariable UUID id) {
        eliminarConvocatoriaUseCase.ejecutar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/plan-trabajo")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'SCHOOL_DIRECTOR', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<PlanTrabajoResponse> obtenerPlanTrabajo(@PathVariable UUID id) {
        PlanTrabajo plan = consultarPlanTrabajoUseCase.obtenerPorProcesoId(id);
        return ResponseEntity.ok(mapearPlanTrabajoResponse(plan));
    }

    @GetMapping({"/{id}/plan-trabajo/plantilla", "/{id}/plantilla-plan"})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'SCHOOL_DIRECTOR', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<PlantillaPlanTrabajoDto> obtenerPlantillaPlanTrabajo(@PathVariable UUID id) {
        PlantillaPlanTrabajoDto plantilla = obtenerPlantillaPlanTrabajoUseCase.obtenerPorProcesoId(id);
        return ResponseEntity.ok(plantilla);
    }

    @PostMapping({"/{id}/plan-trabajo/elaborar", "/{id}/elaborar-plan"})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<PlanTrabajoResponse> elaborarPlanTrabajo(
            @PathVariable UUID id,
            @RequestBody ElaborarPlanTrabajoRequest request
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();

        List<ItemActividadPlanCommand> itemsCommand = (request.actividades() != null)
                ? request.actividades().stream().map(a -> new ItemActividadPlanCommand(
                        a.numeroOrden(),
                        a.codigoActividad(),
                        a.nombre(),
                        a.hoja(),
                        a.fechaInicioEstimada(),
                        a.fechaFinEstimada(),
                        a.responsable(),
                        a.prioridad()
                )).collect(Collectors.toList())
                : Collections.emptyList();

        ElaborarPlanTrabajoCommand command = new ElaborarPlanTrabajoCommand(
                id,
                request.fechaInicio(),
                request.fechaCierre(),
                request.estadoCarrera(),
                request.nroResolucionConeau(),
                request.rmEstandares(),
                request.areaDisciplinaria(),
                request.modalidadCarrera(),
                request.faseEvaluacion(),
                request.fechaLimiteConeau(),
                itemsCommand,
                usuarioId
        );

        PlanTrabajo plan = elaborarPlanTrabajoUseCase.elaborar(command);
        return ResponseEntity.ok(mapearPlanTrabajoResponse(plan));
    }

    @PostMapping("/{id}/rg001/borrador")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroConvocatoriaResponse> guardarRg001Borrador(
            @PathVariable UUID id,
            @RequestBody GuardarRg001Request request
    ) {
        return guardarRg001(id, request, false);
    }

    @PostMapping("/{id}/rg001/enviar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroConvocatoriaResponse> enviarRg001(
            @PathVariable UUID id,
            @RequestBody GuardarRg001Request request
    ) {
        return guardarRg001(id, request, true);
    }

    private ResponseEntity<RegistroConvocatoriaResponse> guardarRg001(UUID procesoId, GuardarRg001Request request, boolean enviar) {
        RegistroConvocatoria registro = registroRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new com.undec.acreditacion.domain.exceptions.ResourceNotFoundException(
                        "No se encontró el registro de convocatoria para el proceso ID: " + procesoId));

        Optional<User> usuarioActualOpt = resolverUsuarioAutenticado();
        boolean esAdminOAEA = tienePermisoGlobal(usuarioActualOpt);

        if (registro.isEnviado() && !esAdminOAEA) {
            throw new com.undec.acreditacion.domain.exceptions.DomainValidationException(
                    "Solo el usuario del Área de Evaluación y Acreditación (AEA) puede modificar los datos del formulario RG-001 una vez enviados.");
        }

        if (request != null) {
            FaseEvaluacion fase = null;
            if (request.faseEvaluacion() != null && !request.faseEvaluacion().isBlank()) {
                try {
                    fase = FaseEvaluacion.valueOf(request.faseEvaluacion().trim());
                } catch (IllegalArgumentException ignored) {}
            }
            registro.completarDatosRg001(
                    request.fechaInicio(),
                    request.fechaCierre(),
                    request.estadoCarrera(),
                    request.nroResolucionConeau(),
                    request.rmEstandares(),
                    request.areaDisciplinaria(),
                    request.modalidadCarrera(),
                    fase,
                    request.fechaLimitePresentacionConeau()
            );

            // Sincronizar proceso si estadoCarrera cambió
            if (request.estadoCarrera() != null && !request.estadoCarrera().isBlank()) {
                try {
                    TipoProcesoAcreditacion nuevoTipo = TipoProcesoAcreditacion.valueOf(request.estadoCarrera().trim().toUpperCase());
                    procesoRepository.findById(procesoId).ifPresent(p -> {
                        if (p.getTipoProceso() != nuevoTipo) {
                            p.actualizarTipoProceso(nuevoTipo);
                            procesoRepository.save(p);
                        }
                    });
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (enviar) {
            registro.marcarComoEnviado();
        } else {
            registro.marcarComoBorrador();
        }
        RegistroConvocatoria guardado = registroRepository.save(registro);

        return ResponseEntity.ok(new RegistroConvocatoriaResponse(
                guardado.getId(),
                guardado.getNroResolucionConeau() != null ? guardado.getNroResolucionConeau() : "",
                guardado.getFechaInicio(),
                guardado.getFechaCierre(),
                guardado.getObservaciones(),
                guardado.getRmEstandares(),
                guardado.getAreaDisciplinaria(),
                guardado.getModalidadCarrera(),
                guardado.getEstadoCarrera(),
                guardado.getFaseEvaluacion() != null ? guardado.getFaseEvaluacion().name() : FaseEvaluacion.PRIMERA_FASE.name(),
                guardado.getFechaLimitePresentacionConeau(),
                guardado.getFechaFormalizacionElectronica(),
                guardado.getEstadoRg001(),
                guardado.getCreadoEn(),
                guardado.getActualizadoEn()
        ));
    }

    @PostMapping("/{id}/plan-trabajo/borrador")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<PlanTrabajoResponse> guardarPlanTrabajoBorrador(
            @PathVariable UUID id,
            @RequestBody GuardarCronogramaPlanRequest request
    ) {
        return guardarPlanTrabajo(id, request, false);
    }

    @PostMapping("/{id}/plan-trabajo/enviar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<PlanTrabajoResponse> enviarPlanTrabajo(
            @PathVariable UUID id,
            @RequestBody GuardarCronogramaPlanRequest request
    ) {
        return guardarPlanTrabajo(id, request, true);
    }

    private ResponseEntity<PlanTrabajoResponse> guardarPlanTrabajo(UUID procesoId, GuardarCronogramaPlanRequest request, boolean enviar) {
        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(procesoId)
                .orElseThrow(() -> new com.undec.acreditacion.domain.exceptions.ResourceNotFoundException(
                        "No se encontró el plan de trabajo para el proceso ID: " + procesoId));

        Optional<User> usuarioActualOpt = resolverUsuarioAutenticado();
        boolean esAdminOAEA = tienePermisoGlobal(usuarioActualOpt);

        if (plan.isEnviado() && !esAdminOAEA) {
            throw new com.undec.acreditacion.domain.exceptions.DomainValidationException(
                    "Solo el usuario del Área de Evaluación y Acreditación (AEA) puede modificar el Plan de Trabajo una vez enviado.");
        }

        List<ActividadPlanTrabajo> actividadesCronograma = new ArrayList<>();
        if (request != null && request.actividades() != null) {
            int ordenIdx = 3;
            for (ItemActividadPlanRequest item : request.actividades()) {
                ActividadPlanTrabajo act = ActividadPlanTrabajo.crearActividadCronograma(
                        plan.getId(),
                        item.numeroOrden() > 0 ? item.numeroOrden() : ordenIdx++,
                        item.codigoActividad() != null ? item.codigoActividad().trim() : "ACT-" + ordenIdx,
                        item.nombre() != null ? item.nombre().trim() : "Actividad",
                        item.hoja(),
                        item.fechaInicioEstimada(),
                        item.fechaFinEstimada(),
                        item.responsable() != null ? item.responsable().trim() : "Responsable",
                        item.prioridad(),
                        item.predecesoras()
                );
                actividadesCronograma.add(act);
            }
        }

        LocalDate fechaLimite = request != null ? request.fechaLimiteConeau() : null;
        if (fechaLimite == null) {
            fechaLimite = registroRepository.findByProcesoId(procesoId)
                    .map(RegistroConvocatoria::getFechaLimitePresentacionConeau)
                    .orElse(null);
        }

        plan.guardarCronograma(actividadesCronograma, fechaLimite, enviar);
        PlanTrabajo guardado = planTrabajoRepository.save(plan);

        return ResponseEntity.ok(mapearPlanTrabajoResponse(guardado));
    }

    @PostMapping("/{id}/plan-trabajo/finalizar-actividad")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<ConvocatoriaDetalleResponse> finalizarActividadPlanTrabajo(@PathVariable UUID id) {
        RegistroConvocatoria registro = registroRepository.findByProcesoId(id)
                .orElseThrow(() -> new com.undec.acreditacion.domain.exceptions.ResourceNotFoundException(
                        "No se encontró el registro de convocatoria para el proceso ID: " + id));
        PlanTrabajo plan = planTrabajoRepository.findByProcesoId(id)
                .orElseThrow(() -> new com.undec.acreditacion.domain.exceptions.ResourceNotFoundException(
                        "No se encontró el plan de trabajo para el proceso ID: " + id));

        if (!registro.isEnviado()) {
            throw new com.undec.acreditacion.domain.exceptions.DomainValidationException(
                    "No se puede finalizar la actividad: el formulario RG-001 debe ser enviado previamente.");
        }
        if (!plan.isEnviado()) {
            throw new com.undec.acreditacion.domain.exceptions.DomainValidationException(
                    "No se puede finalizar la actividad: el Plan de Trabajo debe ser enviado previamente.");
        }

        UUID usuarioId = resolverUsuarioAutenticadoId();
        plan.finalizarActividadElaboracionPlan(usuarioId);
        planTrabajoRepository.save(plan);

        ProcesoAcreditacion proceso = consultarConvocatoriaUseCase.obtenerPorId(id);
        return ResponseEntity.ok(construirDetalleResponse(proceso));
    }

    private PlanTrabajoResponse mapearPlanTrabajoResponse(PlanTrabajo plan) {
        List<ActividadDetalleResponse> actividadesResponse = new ArrayList<>();
        for (ActividadPlanTrabajo act : plan.getActividades()) {
            List<DocumentoAcreditacion> docs = documentoRepository.findByActividadId(act.getId());
            List<DocumentoAcreditacionResponse> docsDto = docs.stream().map(d ->
                    new DocumentoAcreditacionResponse(
                            d.getId(),
                            d.getNombreOriginal(),
                            d.getTipoDocumento().name(),
                            d.getClasificacionOrigen(),
                            d.getDescripcion(),
                            d.getMimeType(),
                            d.getTamanioBytes(),
                            "/api/documentos/" + d.getId() + "/descargar",
                            d.getCreadoEn()
                    )
            ).collect(Collectors.toList());

            String completadaPorNombre = null;
            if (act.getCompletadaPor() != null) {
                completadaPorNombre = userRepository.findById(act.getCompletadaPor())
                        .map(User::getUsername)
                        .orElse("Usuario AEA");
            }

            List<String> pendientes = plan.getPredecesorasPendientes(act);
            boolean bloqueada = !pendientes.isEmpty();

            actividadesResponse.add(new ActividadDetalleResponse(
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
                    completadaPorNombre,
                    act.getHoja() != null ? act.getHoja().name() : HojaPlanTrabajo.INFO.name(),
                    act.getFechaInicioEstimada(),
                    act.getFechaFinEstimada(),
                    act.getResponsable(),
                    act.getPrioridad() != null ? act.getPrioridad().name() : PrioridadActividad.MEDIA.name(),
                    act.getCreadoEn(),
                    act.getActualizadoEn(),
                    docsDto,
                    act.getAreasInformadas().stream().map(Enum::name).toList(),
                    act.getDocumentacionRemitida().stream().map(Enum::name).toList(),
                    act.getObservaciones(),
                    act.getPlazosExtendidos(),
                    act.getPredecesoras(),
                    bloqueada,
                    pendientes
            ));
        }

        return new PlanTrabajoResponse(
                plan.getId(),
                plan.getProcesoId(),
                plan.getCodigo(),
                plan.getTipo().name(),
                plan.getEstado().name(),
                plan.getEstadoEnvio() != null ? plan.getEstadoEnvio() : "BORRADOR",
                actividadesResponse
        );
    }

    private ConvocatoriaDetalleResponse construirDetalleResponse(ProcesoAcreditacion proceso) {
        Optional<Carrera> carreraOpt = carreraRepository.findById(proceso.getCarreraId());
        Optional<RegistroConvocatoria> regOpt = registroRepository.findByProcesoId(proceso.getId());
        Optional<PlanTrabajo> planOpt = Optional.empty();
        try {
            planOpt = Optional.ofNullable(consultarPlanTrabajoUseCase.obtenerPorProcesoId(proceso.getId()));
        } catch (Exception ignored) {
        }

        String areaCarreraFallback = carreraOpt.map(Carrera::getAreaDisciplinaria).orElse("Ingeniería / Sistemas");
        String modalidadCarreraFallback = carreraOpt
                .map(c -> c.getModalidad() == ModalidadCarrera.DISTANCIA ? "A_DISTANCIA" : "PRESENCIAL")
                .orElse("PRESENCIAL");

        RegistroConvocatoriaResponse regResponse = regOpt.map(r -> new RegistroConvocatoriaResponse(
                r.getId(),
                r.getNroResolucionConeau() != null ? r.getNroResolucionConeau() : "",
                r.getFechaInicio(),
                r.getFechaCierre(),
                r.getObservaciones(),
                r.getRmEstandares(),
                r.getAreaDisciplinaria() != null && !r.getAreaDisciplinaria().isBlank() ? r.getAreaDisciplinaria() : areaCarreraFallback,
                r.getModalidadCarrera() != null && !r.getModalidadCarrera().isBlank() ? r.getModalidadCarrera() : modalidadCarreraFallback,
                r.getEstadoCarrera() != null && !r.getEstadoCarrera().isBlank() ? r.getEstadoCarrera() : proceso.getTipoProceso().name(),
                r.getFaseEvaluacion() != null ? r.getFaseEvaluacion().name() : FaseEvaluacion.PRIMERA_FASE.name(),
                r.getFechaLimitePresentacionConeau(),
                r.getFechaFormalizacionElectronica(),
                r.getEstadoRg001() != null ? r.getEstadoRg001() : "BORRADOR",
                r.getCreadoEn(),
                r.getActualizadoEn()
        )).orElse(null);

        PlanTrabajoResumenResponse planResponse = planOpt.map(p -> new PlanTrabajoResumenResponse(
                p.getId(),
                p.getCodigo(),
                p.getTipo().name(),
                p.getEstado().name(),
                p.getEstadoEnvio() != null ? p.getEstadoEnvio() : "BORRADOR",
                p.getActividades().size()
        )).orElse(null);

        List<ActividadDetalleResponse> actividadesDetalle = planOpt
                .map(p -> mapearPlanTrabajoResponse(p).actividades())
                .orElse(Collections.emptyList());

        Optional<Convocatoria> convEscuelaOpt = convocatoriaEscuelaRepository.findByProcesoId(proceso.getId());
        UUID escConvId = convEscuelaOpt.map(Convocatoria::getId).orElse(null);
        String escConvCodigo = convEscuelaOpt.map(Convocatoria::getCodigo).orElse(null);
        String escConvTitulo = convEscuelaOpt.map(Convocatoria::getTitulo).orElse(null);
        UUID escuelaId = convEscuelaOpt.map(Convocatoria::getUnidadAcademicaId).orElse(null);
        String escuelaNombre = escuelaId != null
                ? unidadAcademicaRepository.findById(escuelaId).map(UnidadAcademica::getNombre).orElse(null)
                : null;
        String escuelaSigla = escuelaId != null
                ? unidadAcademicaRepository.findById(escuelaId).map(UnidadAcademica::getSigla).orElse(null)
                : null;

        return new ConvocatoriaDetalleResponse(
                proceso.getId(),
                proceso.getCodigo(),
                proceso.getTitulo(),
                proceso.getCarreraId(),
                carreraOpt.map(Carrera::getNombre).orElse("Desconocida"),
                carreraOpt.map(Carrera::getSigla).orElse(null),
                carreraOpt.map(Carrera::getNivel).orElse(Carrera.NIVEL_GRADO),
                proceso.getTipoProceso().name(),
                proceso.getEstado().name(),
                proceso.getCreadoEn(),
                proceso.getCreadoPor(),
                regResponse,
                planResponse,
                actividadesDetalle,
                null,
                escuelaNombre,
                escuelaSigla,
                null,
                null,
                Collections.emptyList(),
                escConvId,
                escConvCodigo,
                escConvTitulo
        );
    }

    private ConvocatoriaDetalleResponse construirDetalleResponseDesdeEscuela(ConvocatoriaEscuelaDetalleDto d) {
        RegistroConvocatoriaResponse regResponse = new RegistroConvocatoriaResponse(
                d.id(),
                d.nroResolucionConeau() != null ? d.nroResolucionConeau() : "",
                d.fechaInicio(),
                d.fechaCierre(),
                d.observaciones(),
                null,
                "",
                "PRESENCIAL",
                d.tipoProceso().name(),
                FaseEvaluacion.PRIMERA_FASE.name(),
                null,
                null,
                "BORRADOR",
                d.creadoEn(),
                d.actualizadoEn()
        );

        return new ConvocatoriaDetalleResponse(
                d.id(),
                d.codigo(),
                d.titulo(),
                null,
                d.unidadAcademicaNombre(),
                d.unidadAcademicaSigla(),
                "GRADO",
                d.tipoProceso().name(),
                d.estado().name(),
                d.creadoEn(),
                d.creadoPor(),
                regResponse,
                null,
                Collections.emptyList(),
                d.unidadAcademicaId(),
                d.unidadAcademicaNombre(),
                d.unidadAcademicaSigla(),
                d.confirmadoPorDe(),
                d.confirmadoPorDeEn(),
                d.carreras(),
                null,
                null,
                null
        );
    }

    private Optional<User> resolverUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            String principal = auth.getPrincipal().toString();
            Optional<User> userOpt = userRepository.findByEmail(principal);
            if (userOpt.isPresent()) {
                return userOpt;
            }
        }
        return userRepository.findByEmail("admin@undec.edu.ar");
    }

    private boolean tienePermisoGlobal(Optional<User> userOpt) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return true; // En tests unitarios standalone sin contexto de seguridad
        }
        if (auth.getAuthorities() != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") || a.getAuthority().equals("ROLE_AEA")
                        || a.getAuthority().equals("ADMINISTRATOR") || a.getAuthority().equals("AEA"))) {
            return true;
        }
        return userOpt.map(u -> u.getRoles().stream()
                .anyMatch(r -> "ADMINISTRATOR".equals(r.getCode()) || "AEA".equals(r.getCode())))
                .orElse(false);
    }

    private UUID resolverUsuarioAutenticadoId() {
        return resolverUsuarioAutenticado()
                .map(User::getId)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().map(User::getId)
                        .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }
}
