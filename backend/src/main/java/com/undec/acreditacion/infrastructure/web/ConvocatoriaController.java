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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/convocatorias")
public class ConvocatoriaController {

    private final IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase;
    private final ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase;
    private final ListarConvocatoriasUseCase listarConvocatoriasUseCase;
    private final ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase;
    private final RegistroConvocatoriaRepositoryPort registroRepository;
    private final CarreraRepositoryPort carreraRepository;
    private final DocumentoAcreditacionRepositoryPort documentoRepository;
    private final UserRepository userRepository;

    public ConvocatoriaController(
            IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase,
            ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase,
            ListarConvocatoriasUseCase listarConvocatoriasUseCase,
            ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase,
            RegistroConvocatoriaRepositoryPort registroRepository,
            CarreraRepositoryPort carreraRepository,
            DocumentoAcreditacionRepositoryPort documentoRepository,
            UserRepository userRepository) {
        this.iniciarConvocatoriaUseCase = iniciarConvocatoriaUseCase;
        this.consultarConvocatoriaUseCase = consultarConvocatoriaUseCase;
        this.listarConvocatoriasUseCase = listarConvocatoriasUseCase;
        this.consultarPlanTrabajoUseCase = consultarPlanTrabajoUseCase;
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

    @GetMapping("/{id}/plan-trabajo")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<PlanTrabajoResponse> obtenerPlanTrabajo(@PathVariable UUID id) {
        PlanTrabajo plan = consultarPlanTrabajoUseCase.obtenerPorProcesoId(id);

        List<ActividadDetalleResponse> actividadesResponse = new ArrayList<>();
        for (ActividadPlanTrabajo act : plan.getActividades()) {
            List<DocumentoAcreditacion> docs = documentoRepository.findByActividadId(act.getId());
            List<DocumentoAcreditacionResponse> docsDto = docs.stream().map(d ->
                    new DocumentoAcreditacionResponse(
                            d.getId(),
                            d.getNombreOriginal(),
                            d.getTipoDocumento().name(),
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
                    act.getCreadoEn(),
                    act.getActualizadoEn(),
                    docsDto
            ));
        }

        PlanTrabajoResponse response = new PlanTrabajoResponse(
                plan.getId(),
                plan.getProcesoId(),
                plan.getCodigo(),
                plan.getTipo().name(),
                plan.getEstado().name(),
                actividadesResponse
        );

        return ResponseEntity.ok(response);
    }

    private ConvocatoriaDetalleResponse construirDetalleResponse(ProcesoAcreditacion proceso) {
        Optional<Carrera> carreraOpt = carreraRepository.findById(proceso.getCarreraId());
        Optional<RegistroConvocatoria> regOpt = registroRepository.findByProcesoId(proceso.getId());
        Optional<PlanTrabajo> planOpt = Optional.empty();
        try {
            planOpt = Optional.ofNullable(consultarPlanTrabajoUseCase.obtenerPorProcesoId(proceso.getId()));
        } catch (Exception ignored) {
        }

        RegistroConvocatoriaResponse regResponse = regOpt.map(r -> new RegistroConvocatoriaResponse(
                r.getId(),
                r.getNroResolucionConeau(),
                r.getFechaInicio(),
                r.getFechaCierre(),
                r.getObservaciones(),
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
                planResponse
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
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
