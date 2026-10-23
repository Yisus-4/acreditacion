package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;
import com.undec.acreditacion.infrastructure.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/convocatorias", "/api/v1/procesos-acreditacion"})
public class ConvocatoriaController {

    private final IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase;
    private final ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase;
    private final ListarConvocatoriasUseCase listarConvocatoriasUseCase;
    private final EliminarConvocatoriaUseCase eliminarConvocatoriaUseCase;
    private final ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase;
    private final ElaborarPlanTrabajoUseCase elaborarPlanTrabajoUseCase;
    private final ObtenerPlantillaPlanTrabajoUseCase obtenerPlantillaPlanTrabajoUseCase;
    private final RegistroConvocatoriaRepositoryPort registroRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final DocumentoAcreditacionRepositoryPort documentoRepository;
    private final UserRepository userRepository;

    public ConvocatoriaController(
            IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase,
            ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase,
            ListarConvocatoriasUseCase listarConvocatoriasUseCase,
            EliminarConvocatoriaUseCase eliminarConvocatoriaUseCase,
            ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase,
            ElaborarPlanTrabajoUseCase elaborarPlanTrabajoUseCase,
            ObtenerPlantillaPlanTrabajoUseCase obtenerPlantillaPlanTrabajoUseCase,
            RegistroConvocatoriaRepositoryPort registroRepository,
            CarreraRepositoryPort carreraRepository,
            DocumentoAcreditacionRepositoryPort documentoRepository,
            UserRepository userRepository) {
        this.iniciarConvocatoriaUseCase = iniciarConvocatoriaUseCase;
        this.consultarConvocatoriaUseCase = consultarConvocatoriaUseCase;
        this.listarConvocatoriasUseCase = listarConvocatoriasUseCase;
        this.eliminarConvocatoriaUseCase = eliminarConvocatoriaUseCase;
        this.consultarPlanTrabajoUseCase = consultarPlanTrabajoUseCase;
        this.elaborarPlanTrabajoUseCase = elaborarPlanTrabajoUseCase;
        this.obtenerPlantillaPlanTrabajoUseCase = obtenerPlantillaPlanTrabajoUseCase;
        this.registroRepository = registroRepository;
        this.carreraRepository = carreraRepository;
        this.documentoRepository = documentoRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<ConvocatoriaDetalleResponse> iniciarConvocatoria(@RequestBody IniciarConvocatoriaRequest request) {
        UUID usuarioId = resolverUsuarioAutenticadoId();

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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<List<ConvocatoriaResumenResponse>> listarConvocatorias(
            @RequestParam(required = false) UUID carreraId,
            @RequestParam(required = false) EstadoProcesoAcreditacion estado,
            @RequestParam(required = false) TipoProcesoAcreditacion tipoProceso,
            @RequestParam(required = false) Integer anio
    ) {
        ConvocatoriaFiltro filtro = new ConvocatoriaFiltro(carreraId, estado, tipoProceso, anio);
        List<ProcesoAcreditacion> procesos = listarConvocatoriasUseCase.listar(filtro);

        List<ConvocatoriaResumenResponse> response = procesos.stream().map(p -> {
            Optional<Carrera> carreraOpt = carreraRepository.findById(p.getCarreraId());
            Optional<RegistroConvocatoria> regOpt = registroRepository.findByProcesoId(p.getId());
            Optional<PlanTrabajo> planOpt = Optional.empty();
            try {
                planOpt = Optional.ofNullable(consultarPlanTrabajoUseCase.obtenerPorProcesoId(p.getId()));
            } catch (Exception ignored) {
            }

            return new ConvocatoriaResumenResponse(
                    p.getId(),
                    p.getCodigo(),
                    p.getTitulo(),
                    p.getCarreraId(),
                    carreraOpt.map(Carrera::getNombre).orElse("Desconocida"),
                    carreraOpt.map(Carrera::getSigla).orElse(null),
                    carreraOpt.map(Carrera::getNivel).orElse(Carrera.NIVEL_GRADO),
                    regOpt.map(RegistroConvocatoria::getNroResolucionConeau).orElse(""),
                    p.getTipoProceso().name(),
                    p.getEstado().name(),
                    regOpt.map(RegistroConvocatoria::getFechaInicio).orElse(null),
                    regOpt.map(RegistroConvocatoria::getFechaCierre).orElse(null),
                    planOpt.map(PlanTrabajo::getCodigo).orElse(null),
                    p.getCreadoEn()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<ConvocatoriaDetalleResponse> obtenerPorId(@PathVariable UUID id) {
        ProcesoAcreditacion proceso = consultarConvocatoriaUseCase.obtenerPorId(id);
        return ResponseEntity.ok(construirDetalleResponse(proceso));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA')")
    public ResponseEntity<Void> eliminarConvocatoria(@PathVariable UUID id) {
        eliminarConvocatoriaUseCase.ejecutar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/plan-trabajo")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<PlanTrabajoResponse> obtenerPlanTrabajo(@PathVariable UUID id) {
        PlanTrabajo plan = consultarPlanTrabajoUseCase.obtenerPorProcesoId(id);
        return ResponseEntity.ok(mapearPlanTrabajoResponse(plan));
    }

    @GetMapping({"/{id}/plan-trabajo/plantilla", "/{id}/plantilla-plan"})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
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
                    act.getObservaciones()
            ));
        }

        return new PlanTrabajoResponse(
                plan.getId(),
                plan.getProcesoId(),
                plan.getCodigo(),
                plan.getTipo().name(),
                plan.getEstado().name(),
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
                r.getCreadoEn(),
                r.getActualizadoEn()
        )).orElse(null);

        PlanTrabajoResumenResponse planResponse = planOpt.map(p -> new PlanTrabajoResumenResponse(
                p.getId(),
                p.getCodigo(),
                p.getTipo().name(),
                p.getEstado().name(),
                p.getActividades().size()
        )).orElse(null);

        List<ActividadDetalleResponse> actividadesDetalle = planOpt
                .map(p -> mapearPlanTrabajoResponse(p).actividades())
                .orElse(Collections.emptyList());

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
                actividadesDetalle
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
                .orElseGet(() -> userRepository.findAll().stream().findFirst().map(User::getId)
                        .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }
}
