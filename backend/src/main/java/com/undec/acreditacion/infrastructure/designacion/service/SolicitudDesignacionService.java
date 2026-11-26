package com.undec.acreditacion.infrastructure.designacion.service;

import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.SpringDataCuerpoAcademicoMaestroRepository;
import com.undec.acreditacion.infrastructure.designacion.dto.*;
import com.undec.acreditacion.infrastructure.designacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SolicitudDesignacionService {

    public static final int TOPE_HORAS_SEMANALES_DOCENTE = 50;

    private static final Set<String> ROLES_VISION_INSTITUCIONAL = Set.of(
            CodigosRol.AEA,
            CodigosRol.SECRETARIA_ACADEMICA,
            CodigosRol.CONSULTA_INSTITUCIONAL,
            CodigosRol.DIRECTOR_ESCUELA,
            CodigosRol.DIRECTOR_CARRERA
    );

    private final SpringDataSolicitudDesignacionRepository solicitudRepository;
    private final SpringDataSolicitudDocenteDesignacionRepository designacionRepository;
    private final SpringDataSolicitudAsignacionCurricularRepository asignacionRepository;
    private final SpringDataDepartamentoAcademicoRepository departamentoRepository;
    private final SpringDataPeriodoAcademicoRepository periodoRepository;
    private final SpringDataCargoDocenteRepository cargoRepository;
    private final SpringDataDedicacionDocenteRepository dedicacionRepository;
    private final SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository;
    private final SpringDataCuerpoAcademicoMaestroRepository docenteRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final SpringDataActividadCurricularPlanRepository actividadRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final SpringDataSolicitudDesignacionCambioRepository cambioRepository;

    public SolicitudDesignacionService(
            SpringDataSolicitudDesignacionRepository solicitudRepository,
            SpringDataSolicitudDocenteDesignacionRepository designacionRepository,
            SpringDataSolicitudAsignacionCurricularRepository asignacionRepository,
            SpringDataDepartamentoAcademicoRepository departamentoRepository,
            SpringDataPeriodoAcademicoRepository periodoRepository,
            SpringDataCargoDocenteRepository cargoRepository,
            SpringDataDedicacionDocenteRepository dedicacionRepository,
            SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository,
            SpringDataCuerpoAcademicoMaestroRepository docenteRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this(
                solicitudRepository,
                designacionRepository,
                asignacionRepository,
                departamentoRepository,
                periodoRepository,
                cargoRepository,
                dedicacionRepository,
                tipoDesignacionRepository,
                docenteRepository,
                carreraRepository,
                planRepository,
                actividadRepository,
                authenticatedUserProvider,
                null
        );
    }

    @Autowired
    public SolicitudDesignacionService(
            SpringDataSolicitudDesignacionRepository solicitudRepository,
            SpringDataSolicitudDocenteDesignacionRepository designacionRepository,
            SpringDataSolicitudAsignacionCurricularRepository asignacionRepository,
            SpringDataDepartamentoAcademicoRepository departamentoRepository,
            SpringDataPeriodoAcademicoRepository periodoRepository,
            SpringDataCargoDocenteRepository cargoRepository,
            SpringDataDedicacionDocenteRepository dedicacionRepository,
            SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository,
            SpringDataCuerpoAcademicoMaestroRepository docenteRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataPlanEstudioFt001Repository planRepository,
            SpringDataActividadCurricularPlanRepository actividadRepository,
            AuthenticatedUserProvider authenticatedUserProvider,
            SpringDataSolicitudDesignacionCambioRepository cambioRepository
    ) {
        this.solicitudRepository = Objects.requireNonNull(solicitudRepository);
        this.designacionRepository = Objects.requireNonNull(designacionRepository);
        this.asignacionRepository = Objects.requireNonNull(asignacionRepository);
        this.departamentoRepository = Objects.requireNonNull(departamentoRepository);
        this.periodoRepository = Objects.requireNonNull(periodoRepository);
        this.cargoRepository = Objects.requireNonNull(cargoRepository);
        this.dedicacionRepository = Objects.requireNonNull(dedicacionRepository);
        this.tipoDesignacionRepository = Objects.requireNonNull(tipoDesignacionRepository);
        this.docenteRepository = Objects.requireNonNull(docenteRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.planRepository = Objects.requireNonNull(planRepository);
        this.actividadRepository = Objects.requireNonNull(actividadRepository);
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.cambioRepository = cambioRepository;
    }

    @Transactional(readOnly = true)
    public List<SolicitudDesignacionResumenDto> listarSolicitudes(UUID periodoId, UUID departamentoId) {
        UUID deptoEfectivo = resolverDepartamentoLectura(departamentoId);
        List<SolicitudDesignacionJpaEntity> solicitudes;
        if (deptoEfectivo != null && periodoId != null) {
            solicitudes = solicitudRepository.findByDepartamentoAcademicoIdAndPeriodoAcademicoIdOrderByCreadoEnDesc(
                    deptoEfectivo, periodoId);
        } else if (deptoEfectivo != null) {
            solicitudes = solicitudRepository.findByDepartamentoAcademicoIdOrderByCreadoEnDesc(deptoEfectivo);
        } else if (periodoId != null) {
            solicitudes = solicitudRepository.findByPeriodoAcademicoIdOrderByCreadoEnDesc(periodoId);
        } else {
            solicitudes = solicitudRepository.findAllByOrderByCreadoEnDesc();
        }
        return solicitudes.stream().map(this::toResumenDto).toList();
    }

    @Transactional(readOnly = true)
    public SolicitudDesignacionDetalleDto obtenerDetalleSolicitud(UUID solicitudId) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarAccesoLecturaDepartamento(solicitud.getDepartamentoAcademico().getId());
        return toDetalleDto(solicitud);
    }

    @Transactional(readOnly = true)
    public List<CambioSolicitudDesignacionDto> listarHistorialCambios(UUID solicitudId) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarAccesoLecturaDepartamento(solicitud.getDepartamentoAcademico().getId());
        return obtenerHistorialDtos(solicitudId);
    }

    @Transactional(readOnly = true)
    public VistaConsolidadaPeriodoDto obtenerVistaConsolidadaPeriodo(UUID departamentoId, UUID periodoId) {
        if (periodoId == null) {
            throw new DomainValidationException("El Período Académico es obligatorio para consultar el consolidado.");
        }
        UUID deptoEfectivo = resolverDepartamentoLectura(departamentoId);
        if (deptoEfectivo == null) {
            throw new DomainValidationException("El Departamento Académico es obligatorio para consultar el consolidado.");
        }
        validarAccesoLecturaDepartamento(deptoEfectivo);

        DepartamentoAcademicoJpaEntity depto = departamentoRepository.findById(deptoEfectivo)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el Departamento Académico ID: " + deptoEfectivo));
        PeriodoAcademicoJpaEntity periodo = periodoRepository.findById(periodoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el Período Académico ID: " + periodoId));

        List<SolicitudDesignacionJpaEntity> solicitudesPeriodo =
                solicitudRepository.findByDepartamentoAcademicoIdAndPeriodoAcademicoIdOrderByCreadoEnDesc(depto.getId(), periodo.getId());

        List<SolicitudDocenteDesignacionJpaEntity> todosActivos = new ArrayList<>();
        for (SolicitudDesignacionJpaEntity s : solicitudesPeriodo) {
            for (SolicitudDocenteDesignacionJpaEntity d : designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(s.getId())) {
                if (d.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO) {
                    todosActivos.add(d);
                }
            }
        }

        Set<UUID> reemplazadosEnPeriodo = todosActivos.stream()
                .map(SolicitudDocenteDesignacionJpaEntity::getDesignacionOrigenId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<SolicitudDocenteDesignacionJpaEntity> vigentes = todosActivos.stream()
                .filter(d -> !reemplazadosEnPeriodo.contains(d.getId()))
                .toList();

        List<UUID> vigenteIds = vigentes.stream().map(SolicitudDocenteDesignacionJpaEntity::getId).toList();
        Map<UUID, List<SolicitudAsignacionCurricularJpaEntity>> asigsMap = vigenteIds.isEmpty()
                ? Map.of()
                : asignacionRepository.findByDesignacionIdInOrderByCreadoEnAsc(vigenteIds).stream()
                        .collect(Collectors.groupingBy(a -> a.getDesignacion().getId()));

        Map<UUID, Integer> totalPorDocente = new HashMap<>();
        for (SolicitudDocenteDesignacionJpaEntity d : vigentes) {
            totalPorDocente.merge(d.getDocente().getId(), d.getHorasDedicacion() != null ? d.getHorasDedicacion() : 0, Integer::sum);
        }

        List<DesignacionDocenteItemDto> dtos = vigentes.stream()
                .map(d -> toDesignacionDto(
                        d,
                        asigsMap.getOrDefault(d.getId(), List.of()),
                        totalPorDocente.getOrDefault(d.getDocente().getId(), 0)))
                .toList();

        return new VistaConsolidadaPeriodoDto(
                depto.getId(),
                depto.getDenominacion(),
                periodo.getId(),
                periodo.getDenominacion(),
                periodo.getAnio(),
                solicitudesPeriodo.size(),
                dtos.size(),
                dtos
        );
    }

    public SolicitudDesignacionDetalleDto crearSolicitud(CrearSolicitudDesignacionRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la solicitud son obligatorios.");
        }
        if (req.periodoAcademicoId() == null) {
            throw new DomainValidationException("El Período Académico es obligatorio.");
        }
        UUID deptoId = resolverDepartamentoEscritura(req.departamentoAcademicoId());
        if (deptoId == null) {
            throw new DomainValidationException("El Departamento Académico es obligatorio.");
        }

        DepartamentoAcademicoJpaEntity depto = departamentoRepository.findById(deptoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el Departamento Académico ID: " + deptoId));
        if (!depto.isActivo()) {
            throw new DomainValidationException("El Departamento Académico '" + depto.getDenominacion() + "' se encuentra inactivo.");
        }
        PeriodoAcademicoJpaEntity periodo = periodoRepository.findById(req.periodoAcademicoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el Período Académico ID: " + req.periodoAcademicoId()));
        if (!periodo.isActivo()) {
            throw new DomainValidationException("El Período Académico '" + periodo.getDenominacion() + "' se encuentra inactivo.");
        }

        TipoSolicitudDesignacion tipo = req.tipoSolicitud() != null ? req.tipoSolicitud() : TipoSolicitudDesignacion.ANUAL;
        if (tipo == TipoSolicitudDesignacion.ANUAL) {
            boolean yaExiste = solicitudRepository
                    .findByDepartamentoAcademicoIdAndPeriodoAcademicoIdAndTipoSolicitud(depto.getId(), periodo.getId(), TipoSolicitudDesignacion.ANUAL)
                    .isPresent();
            if (yaExiste) {
                throw new DomainValidationException(
                        "Ya existe una solicitud ANUAL para el departamento '" + depto.getDenominacion()
                                + "' en el período '" + periodo.getDenominacion() + "'.");
            }
        }

        SolicitudDesignacionJpaEntity origenClonacion = null;
        UUID solicitudOrigenVinculadaId = null;

        boolean deseaClonar = Boolean.TRUE.equals(req.clonarPeriodoAnterior()) || req.solicitudOrigenId() != null;
        if (deseaClonar && tipo == TipoSolicitudDesignacion.ANUAL) {
            origenClonacion = resolverSolicitudOrigenClonacion(depto, periodo, req.solicitudOrigenId());
            if (origenClonacion != null) {
                solicitudOrigenVinculadaId = origenClonacion.getId();
            }
        } else if (tipo == TipoSolicitudDesignacion.EXTRAORDINARIA) {
            if (req.solicitudOrigenId() != null) {
                SolicitudDesignacionJpaEntity ref = obtenerSolicitudOCascada(req.solicitudOrigenId());
                if (!ref.getDepartamentoAcademico().getId().equals(depto.getId())) {
                    throw new DomainValidationException("La solicitud base vinculada no pertenece al mismo Departamento Académico.");
                }
                if (!ref.getPeriodoAcademico().getId().equals(periodo.getId())) {
                    throw new DomainValidationException("La solicitud base de una EXTRAORDINARIA debe pertenecer al mismo Período Académico.");
                }
                solicitudOrigenVinculadaId = ref.getId();
            } else {
                solicitudOrigenVinculadaId = solicitudRepository
                        .findByDepartamentoAcademicoIdAndPeriodoAcademicoIdAndTipoSolicitud(
                                depto.getId(), periodo.getId(), TipoSolicitudDesignacion.ANUAL)
                        .map(SolicitudDesignacionJpaEntity::getId)
                        .orElse(null);
            }
        }

        SolicitudDesignacionJpaEntity entity = new SolicitudDesignacionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setDepartamentoAcademico(depto);
        entity.setPeriodoAcademico(periodo);
        entity.setTipoSolicitud(tipo);
        entity.setEstado(EstadoSolicitudDesignacion.BORRADOR);
        entity.setSolicitudOrigenId(solicitudOrigenVinculadaId);
        entity.setObservacionesDepartamento(trimToNull(req.observacionesDepartamento()));
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        SolicitudDesignacionJpaEntity saved = solicitudRepository.save(entity);
        registrarCambio(
                saved,
                null,
                TipoEventoSolicitudDesignacion.CREACION,
                "estado",
                null,
                "BORRADOR (" + tipo + ")",
                trimToNull(req.observacionesDepartamento())
        );

        if (origenClonacion != null) {
            ejecutarClonacionDesdeOrigen(saved, origenClonacion);
        }

        return toDetalleDto(saved);
    }

    public DesignacionDocenteItemDto agregarDesignacion(UUID solicitudId, GuardarDesignacionDocenteRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la designación son obligatorios.");
        }
        if (req.docenteId() == null || req.cargoDocenteId() == null
                || req.dedicacionDocenteId() == null || req.tipoDesignacionId() == null) {
            throw new DomainValidationException("Docente, Cargo, Dedicación y Tipo de Designación son obligatorios.");
        }

        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        if (solicitud.getEstado() == EstadoSolicitudDesignacion.OBSERVADA || req.designacionReemplazadaId() != null) {
            exigirMotivoNoVacio(req.motivoCambio(), "registrar este movimiento de designación");
        }

        CuerpoAcademicoMaestroJpaEntity docente = docenteRepository.findById(req.docenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el docente ID: " + req.docenteId()));
        validarDocenteEnDepartamentoSolicitud(docente, solicitud);

        CargoDocenteJpaEntity cargo = cargoRepository.findById(req.cargoDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cargo docente ID: " + req.cargoDocenteId()));
        DedicacionDocenteJpaEntity dedicacion = dedicacionRepository.findById(req.dedicacionDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la dedicación docente ID: " + req.dedicacionDocenteId()));
        TipoDesignacionDocenteJpaEntity tipoDesig = tipoDesignacionRepository.findById(req.tipoDesignacionId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de designación ID: " + req.tipoDesignacionId()));

        validarEntidadesActivas(docente, cargo, dedicacion, tipoDesig);

        UUID designacionReemplazadaId = null;
        if (req.designacionReemplazadaId() != null) {
            if (solicitud.getTipoSolicitud() != TipoSolicitudDesignacion.EXTRAORDINARIA) {
                throw new DomainValidationException("El reemplazo de un cargo vigente solo aplica en solicitudes EXTRAORDINARIAS (RF09).");
            }
            SolicitudDocenteDesignacionJpaEntity reemplazada = designacionRepository.findById(req.designacionReemplazadaId())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró la designación a reemplazar ID: " + req.designacionReemplazadaId()));
            if (!reemplazada.getDocente().getId().equals(docente.getId())
                    || !reemplazada.getSolicitud().getPeriodoAcademico().getId().equals(solicitud.getPeriodoAcademico().getId())
                    || !reemplazada.getSolicitud().getDepartamentoAcademico().getId().equals(solicitud.getDepartamentoAcademico().getId())) {
                throw new DomainValidationException("La designación a reemplazar debe pertenecer al mismo docente, departamento y período académico.");
            }
            designacionReemplazadaId = reemplazada.getId();
        }

        int horasNuevas = dedicacion.getHorasSemanales() != null ? dedicacion.getHorasSemanales() : 0;
        if (horasNuevas <= 0) {
            throw new DomainValidationException("La dedicación seleccionada debe tener una carga horaria mayor a 0 hs semanales.");
        }
        int horasPreviasPeriodo = sumarHorasActivasDocenteEnPeriodo(
                docente.getId(), solicitud.getPeriodoAcademico().getId(), designacionReemplazadaId);
        validarTope50Horas(docente, horasPreviasPeriodo, horasNuevas);

        SolicitudDocenteDesignacionJpaEntity entity = new SolicitudDocenteDesignacionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setSolicitud(solicitud);
        entity.setDesignacionOrigenId(designacionReemplazadaId);
        entity.setDocente(docente);
        entity.setCargoDocente(cargo);
        entity.setDedicacionDocente(dedicacion);
        entity.setTipoDesignacion(tipoDesig);
        entity.setHorasDedicacion(horasNuevas);
        entity.setNroDesignacionMapuche(trimToNull(req.nroDesignacionMapuche()));
        entity.setEstadoItem(designacionReemplazadaId != null ? EstadoItemDesignacion.MODIFICADO : EstadoItemDesignacion.NUEVO);
        entity.setObservaciones(trimToNull(req.observaciones()));
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        SolicitudDocenteDesignacionJpaEntity saved = designacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());

        registrarCambio(
                solicitud,
                saved.getId(),
                TipoEventoSolicitudDesignacion.ALTA_DESIGNACION,
                designacionReemplazadaId != null ? "reemplazo_extraordinario" : "designacion",
                null,
                resumenDesignacion(saved),
                trimToNull(req.motivoCambio())
        );

        int totalDocentePeriodo = horasPreviasPeriodo + horasNuevas;
        List<SolicitudAsignacionCurricularJpaEntity> asignaciones =
                asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(saved.getId());
        return toDesignacionDto(saved, asignaciones, totalDocentePeriodo);
    }

    public DesignacionDocenteItemDto actualizarDesignacion(
            UUID solicitudId,
            UUID designacionId,
            GuardarDesignacionDocenteRequest req
    ) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la designación son obligatorios.");
        }
        if (req.cargoDocenteId() == null || req.dedicacionDocenteId() == null || req.tipoDesignacionId() == null) {
            throw new DomainValidationException("Cargo, Dedicación y Tipo de Designación son obligatorios.");
        }

        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);

        SolicitudDocenteDesignacionJpaEntity entity = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        validarItemNoExcluido(entity);
        if (requiereMotivoAuditoria(solicitud, entity)) {
            exigirMotivoNoVacio(req.motivoCambio(), "modificar un cargo replicado o en solicitud observada");
        }

        CargoDocenteJpaEntity cargo = cargoRepository.findById(req.cargoDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cargo docente ID: " + req.cargoDocenteId()));
        DedicacionDocenteJpaEntity dedicacion = dedicacionRepository.findById(req.dedicacionDocenteId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la dedicación docente ID: " + req.dedicacionDocenteId()));
        TipoDesignacionDocenteJpaEntity tipoDesig = tipoDesignacionRepository.findById(req.tipoDesignacionId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de designación ID: " + req.tipoDesignacionId()));

        validarEntidadesActivas(entity.getDocente(), cargo, dedicacion, tipoDesig);

        int horasNuevas = dedicacion.getHorasSemanales() != null ? dedicacion.getHorasSemanales() : 0;
        if (horasNuevas <= 0) {
            throw new DomainValidationException("La dedicación seleccionada debe tener una carga horaria mayor a 0 hs semanales.");
        }
        if (entity.totalHorasDistribuidas() > horasNuevas) {
            throw new DomainValidationException(
                    "No se puede reducir la dedicación a " + horasNuevas + " hs semanales porque ya hay "
                            + entity.totalHorasDistribuidas() + " hs distribuidas en actividades. Ajuste la distribución horaria primero (RN06).");
        }

        int horasOtrasDesignaciones = sumarHorasActivasDocenteEnPeriodo(
                entity.getDocente().getId(), solicitud.getPeriodoAcademico().getId(), entity.getId());
        validarTope50Horas(entity.getDocente(), horasOtrasDesignaciones, horasNuevas);

        String anterior = resumenDesignacion(entity);

        entity.setCargoDocente(cargo);
        entity.setDedicacionDocente(dedicacion);
        entity.setTipoDesignacion(tipoDesig);
        entity.setHorasDedicacion(horasNuevas);
        entity.setNroDesignacionMapuche(trimToNull(req.nroDesignacionMapuche()));
        entity.setObservaciones(trimToNull(req.observaciones()));
        if (entity.getEstadoItem() == EstadoItemDesignacion.REPLICADO) {
            entity.setEstadoItem(EstadoItemDesignacion.MODIFICADO);
        }
        entity.setActualizadoEn(Instant.now());

        SolicitudDocenteDesignacionJpaEntity saved = designacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());

        registrarCambio(
                solicitud,
                saved.getId(),
                TipoEventoSolicitudDesignacion.MODIFICACION_DESIGNACION,
                "cargo_dedicacion_tipo",
                anterior,
                resumenDesignacion(saved),
                trimToNull(req.motivoCambio())
        );

        int totalDocentePeriodo = horasOtrasDesignaciones + horasNuevas;
        List<SolicitudAsignacionCurricularJpaEntity> asignaciones =
                asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(saved.getId());
        return toDesignacionDto(saved, asignaciones, totalDocentePeriodo);
    }

    public DesignacionDocenteItemDto excluirDesignacion(
            UUID solicitudId,
            UUID designacionId,
            MotivoCambioRequest req
    ) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity entity = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        if (entity.getEstadoItem() == EstadoItemDesignacion.EXCLUIDO) {
            throw new DomainValidationException("La designación ya se encuentra excluida.");
        }
        String motivo = req != null ? req.motivo() : null;
        exigirMotivoNoVacio(motivo, "excluir una designación de la solicitud (RF07.6 / RF08)");

        EstadoItemDesignacion estadoPrevio = entity.getEstadoItem();
        entity.setEstadoItem(EstadoItemDesignacion.EXCLUIDO);
        entity.setActualizadoEn(Instant.now());

        SolicitudDocenteDesignacionJpaEntity saved = designacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());

        registrarCambio(
                solicitud,
                saved.getId(),
                TipoEventoSolicitudDesignacion.EXCLUSION_DESIGNACION,
                "estadoItem",
                estadoPrevio.name() + " (" + resumenDesignacion(saved) + ")",
                EstadoItemDesignacion.EXCLUIDO.name(),
                motivo.trim()
        );

        int totalDocentePeriodo = sumarHorasActivasDocenteEnPeriodo(
                saved.getDocente().getId(), solicitud.getPeriodoAcademico().getId(), saved.getId());
        List<SolicitudAsignacionCurricularJpaEntity> asignaciones =
                asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(saved.getId());
        return toDesignacionDto(saved, asignaciones, totalDocentePeriodo);
    }

    public DesignacionDocenteItemDto reactivarDesignacion(
            UUID solicitudId,
            UUID designacionId,
            MotivoCambioRequest req
    ) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity entity = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        if (entity.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO) {
            throw new DomainValidationException("Solo se pueden reactivar designaciones en estado EXCLUIDO.");
        }
        String motivo = req != null ? req.motivo() : null;
        if (solicitud.getEstado() == EstadoSolicitudDesignacion.OBSERVADA || entity.getDesignacionOrigenId() != null) {
            exigirMotivoNoVacio(motivo, "reactivar una designación excluida (RF08)");
        }

        validarEntidadesActivas(
                entity.getDocente(),
                entity.getCargoDocente(),
                entity.getDedicacionDocente(),
                entity.getTipoDesignacion()
        );

        int horasReactivadas = entity.getHorasDedicacion() != null ? entity.getHorasDedicacion() : 0;
        int horasOtrasActivas = sumarHorasActivasDocenteEnPeriodo(
                entity.getDocente().getId(), solicitud.getPeriodoAcademico().getId(), entity.getId());
        validarTope50Horas(entity.getDocente(), horasOtrasActivas, horasReactivadas);

        List<SolicitudAsignacionCurricularJpaEntity> asignaciones =
                asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(entity.getId());
        for (SolicitudAsignacionCurricularJpaEntity asig : asignaciones) {
            validarCoherenciaRN09(asig.getActividadCurricular(), asig.getModalidadParticipacion());
        }

        EstadoItemDesignacion nuevoEstado = entity.getDesignacionOrigenId() != null
                ? EstadoItemDesignacion.MODIFICADO
                : EstadoItemDesignacion.NUEVO;
        entity.setEstadoItem(nuevoEstado);
        entity.setActualizadoEn(Instant.now());

        SolicitudDocenteDesignacionJpaEntity saved = designacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());

        registrarCambio(
                solicitud,
                saved.getId(),
                TipoEventoSolicitudDesignacion.REACTIVACION_DESIGNACION,
                "estadoItem",
                EstadoItemDesignacion.EXCLUIDO.name(),
                nuevoEstado.name() + " (" + resumenDesignacion(saved) + ")",
                trimToNull(motivo)
        );

        int totalDocentePeriodo = horasOtrasActivas + horasReactivadas;
        return toDesignacionDto(saved, asignaciones, totalDocentePeriodo);
    }

    public void eliminarDesignacion(UUID solicitudId, UUID designacionId) {
        eliminarDesignacion(solicitudId, designacionId, null);
    }

    public void eliminarDesignacion(UUID solicitudId, UUID designacionId, String motivo) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity entity = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        if (entity.getDesignacionOrigenId() != null || entity.getEstadoItem() == EstadoItemDesignacion.REPLICADO) {
            throw new DomainValidationException(
                    "Las designaciones replicadas del período anterior no deben eliminarse físicamente; utilice la opción Excluir para conservar el historial (RF07.6).");
        }
        if (solicitud.getEstado() == EstadoSolicitudDesignacion.OBSERVADA) {
            exigirMotivoNoVacio(motivo, "eliminar una designación en solicitud observada");
        }
        String resumen = resumenDesignacion(entity);
        designacionRepository.delete(entity);
        solicitud.setActualizadoEn(Instant.now());
        registrarCambio(
                solicitud,
                null,
                TipoEventoSolicitudDesignacion.EXCLUSION_DESIGNACION,
                "designacion",
                resumen,
                "ELIMINADA",
                trimToNull(motivo)
        );
    }

    public DesignacionDocenteItemDto actualizarDistribucionHoraria(
            UUID solicitudId,
            UUID designacionId,
            GuardarDistribucionHorariaRequest req
    ) {
        if (req == null) {
            throw new DomainValidationException("La distribución horaria es obligatoria.");
        }
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity entity = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        validarItemNoExcluido(entity);
        if (requiereMotivoAuditoria(solicitud, entity)) {
            exigirMotivoNoVacio(req.motivoCambio(), "modificar la distribución horaria de un cargo replicado o en solicitud observada");
        }

        int clases = validarHoraNoNegativa(req.horasClases(), "Desarrollo de clases");
        int inv = validarHoraNoNegativa(req.horasInvestigacion(), "Investigación");
        int ext = validarHoraNoNegativa(req.horasExtension(), "Extensión");
        int gest = validarHoraNoNegativa(req.horasGestion(), "Gestión");
        int tesis = validarHoraNoNegativa(req.horasDireccionTesis(), "Dirección de tesis/TF");
        int otras = validarHoraNoNegativa(req.horasOtras(), "Otras actividades académicas");

        int total = clases + inv + ext + gest + tesis + otras;
        int topeDedicacion = entity.getHorasDedicacion() != null ? entity.getHorasDedicacion() : 0;
        if (total > topeDedicacion) {
            throw new DomainValidationException(
                    "La suma de horas distribuidas (" + total + " hs) supera la carga horaria de la dedicación ("
                            + topeDedicacion + " hs semanales) (RN06).");
        }

        String anterior = resumenDistribucion(entity);

        entity.setHorasClases(clases);
        entity.setHorasInvestigacion(inv);
        entity.setHorasExtension(ext);
        entity.setHorasGestion(gest);
        entity.setHorasDireccionTesis(tesis);
        entity.setHorasOtras(otras);
        if (entity.getEstadoItem() == EstadoItemDesignacion.REPLICADO) {
            entity.setEstadoItem(EstadoItemDesignacion.MODIFICADO);
        }
        entity.setActualizadoEn(Instant.now());

        SolicitudDocenteDesignacionJpaEntity saved = designacionRepository.save(entity);
        solicitud.setActualizadoEn(Instant.now());

        registrarCambio(
                solicitud,
                saved.getId(),
                TipoEventoSolicitudDesignacion.MODIFICACION_DISTRIBUCION,
                "distribucion_horaria",
                anterior,
                resumenDistribucion(saved),
                trimToNull(req.motivoCambio())
        );

        int totalDocentePeriodo = sumarHorasActivasDocenteEnPeriodo(
                saved.getDocente().getId(), solicitud.getPeriodoAcademico().getId(), null);
        List<SolicitudAsignacionCurricularJpaEntity> asignaciones =
                asignacionRepository.findByDesignacionIdOrderByCreadoEnAsc(saved.getId());
        return toDesignacionDto(saved, asignaciones, totalDocentePeriodo);
    }

    public AsignacionCurricularPeriodoDto agregarAsignacionCurricular(
            UUID solicitudId,
            UUID designacionId,
            AgregarAsignacionCurricularRequest req
    ) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la asignación curricular son obligatorios.");
        }
        if (req.carreraId() == null || req.planEstudioId() == null || req.actividadCurricularId() == null) {
            throw new DomainValidationException("Carrera, Plan de Estudio y Asignatura son obligatorios.");
        }
        if (req.horasAsignadas() != null && req.horasAsignadas() < 0) {
            throw new DomainValidationException("Las horas asignadas a la asignatura no pueden ser negativas.");
        }

        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity designacion = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        validarItemNoExcluido(designacion);
        if (requiereMotivoAuditoria(solicitud, designacion)) {
            exigirMotivoNoVacio(req.motivoCambio(), "agregar asignaturas a un cargo replicado o en solicitud observada");
        }

        CarreraJpaEntity carrera = carreraRepository.findById(req.carreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera ID: " + req.carreraId()));
        PlanEstudioFt001JpaEntity plan = planRepository.findById(req.planEstudioId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el plan de estudio ID: " + req.planEstudioId()));
        ActividadCurricularPlanJpaEntity actividad = actividadRepository.findById(req.actividadCurricularId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignatura ID: " + req.actividadCurricularId()));

        if (plan.getCarrera() != null && !plan.getCarrera().getId().equals(carrera.getId())) {
            throw new DomainValidationException("El plan de estudio seleccionado no pertenece a la carrera indicada.");
        }
        if (actividad.getPlanEstudio() != null && !actividad.getPlanEstudio().getId().equals(plan.getId())) {
            throw new DomainValidationException("La asignatura seleccionada no pertenece al plan de estudio indicado.");
        }

        ModalidadParticipacionDocente modalidad = req.modalidadParticipacion() != null
                ? req.modalidadParticipacion()
                : ModalidadParticipacionDocente.DESARROLLO_CLASES;

        validarCoherenciaRN09(actividad, modalidad);

        boolean duplicada = asignacionRepository.existsByDesignacionIdAndActividadCurricularIdAndModalidadParticipacion(
                designacion.getId(), actividad.getId(), modalidad);
        if (duplicada) {
            throw new DomainValidationException(
                    "El docente ya tiene asignada la asignatura '" + actividad.getNombre()
                            + "' con modalidad " + modalidad + " en esta designación.");
        }

        SolicitudAsignacionCurricularJpaEntity entity = new SolicitudAsignacionCurricularJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setDesignacion(designacion);
        entity.setCarrera(carrera);
        entity.setPlanEstudio(plan);
        entity.setActividadCurricular(actividad);
        entity.setModalidadParticipacion(modalidad);
        entity.setHorasAsignadas(req.horasAsignadas());
        entity.setObservaciones(trimToNull(req.observaciones()));
        entity.setCreadoEn(Instant.now());

        SolicitudAsignacionCurricularJpaEntity saved = asignacionRepository.save(entity);
        if (designacion.getEstadoItem() == EstadoItemDesignacion.REPLICADO) {
            designacion.setEstadoItem(EstadoItemDesignacion.MODIFICADO);
            designacion.setActualizadoEn(Instant.now());
            designacionRepository.save(designacion);
        }
        solicitud.setActualizadoEn(Instant.now());

        registrarCambio(
                solicitud,
                designacion.getId(),
                TipoEventoSolicitudDesignacion.ALTA_ASIGNACION,
                "asignacion_curricular",
                null,
                actividad.getNombre() + " [" + modalidad + "]",
                trimToNull(req.motivoCambio())
        );

        return toAsignacionDto(saved);
    }

    public void eliminarAsignacionCurricular(UUID solicitudId, UUID designacionId, UUID asignacionId) {
        eliminarAsignacionCurricular(solicitudId, designacionId, asignacionId, null);
    }

    public void eliminarAsignacionCurricular(
            UUID solicitudId,
            UUID designacionId,
            UUID asignacionId,
            String motivo
    ) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);
        SolicitudDocenteDesignacionJpaEntity designacion = obtenerDesignacionEnSolicitud(solicitudId, designacionId);
        validarItemNoExcluido(designacion);
        if (requiereMotivoAuditoria(solicitud, designacion)) {
            exigirMotivoNoVacio(motivo, "quitar una asignatura de un cargo replicado o en solicitud observada");
        }

        SolicitudAsignacionCurricularJpaEntity asig = asignacionRepository.findById(asignacionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la asignación curricular ID: " + asignacionId));
        if (!asig.getDesignacion().getId().equals(designacion.getId())) {
            throw new ResourceNotFoundException("La asignación curricular no pertenece a la designación indicada.");
        }
        String anterior = asig.getActividadCurricular().getNombre() + " [" + asig.getModalidadParticipacion() + "]";
        asignacionRepository.delete(asig);
        if (designacion.getEstadoItem() == EstadoItemDesignacion.REPLICADO) {
            designacion.setEstadoItem(EstadoItemDesignacion.MODIFICADO);
            designacion.setActualizadoEn(Instant.now());
            designacionRepository.save(designacion);
        }
        solicitud.setActualizadoEn(Instant.now());

        registrarCambio(
                solicitud,
                designacion.getId(),
                TipoEventoSolicitudDesignacion.BAJA_ASIGNACION,
                "asignacion_curricular",
                anterior,
                null,
                trimToNull(motivo)
        );
    }

    // --- Flujo Formal con Secretaría de Gestión Académica (SGA — RF09 / RN15) ---

    public SolicitudDesignacionDetalleDto presentarSolicitudASga(UUID solicitudId) {
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        validarEscrituraSobreSolicitud(solicitud);

        long activas = designacionRepository.countBySolicitudIdAndEstadoItemNot(
                solicitud.getId(), EstadoItemDesignacion.EXCLUIDO);
        if (activas <= 0) {
            throw new DomainValidationException(
                    "No se puede presentar la solicitud a Secretaría de Gestión Académica porque no contiene designaciones docentes activas.");
        }

        EstadoSolicitudDesignacion estadoPrevio = solicitud.getEstado();
        Instant ahora = Instant.now();
        solicitud.setEstado(EstadoSolicitudDesignacion.PRESENTADA_SGA);
        solicitud.setFechaEmision(ahora);
        solicitud.setEmitidaPorUsuarioId(obtenerUsuarioIdActual());
        solicitud.setActualizadoEn(ahora);

        SolicitudDesignacionJpaEntity saved = solicitudRepository.save(solicitud);
        registrarCambio(
                saved,
                null,
                TipoEventoSolicitudDesignacion.PRESENTACION_SGA,
                "estado",
                estadoPrevio.name(),
                EstadoSolicitudDesignacion.PRESENTADA_SGA.name(),
                "Presentada por Departamento a SGA"
        );
        return toDetalleDto(saved);
    }

    public SolicitudDesignacionDetalleDto aprobarSolicitudSga(UUID solicitudId, RevisionSgaRequest req) {
        exigirRolSgaOAdmin();
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        if (solicitud.getEstado() != EstadoSolicitudDesignacion.PRESENTADA_SGA) {
            throw new DomainValidationException(
                    "Solo se pueden aprobar solicitudes en estado PRESENTADA_SGA (estado actual: " + solicitud.getEstado() + ").");
        }

        Instant ahora = Instant.now();
        String obs = req != null ? trimToNull(req.observacionesSga()) : null;
        solicitud.setEstado(EstadoSolicitudDesignacion.APROBADA);
        solicitud.setObservacionesSga(obs);
        solicitud.setRevisadaPorUsuarioId(obtenerUsuarioIdActual());
        solicitud.setFechaRevision(ahora);
        solicitud.setActualizadoEn(ahora);

        SolicitudDesignacionJpaEntity saved = solicitudRepository.save(solicitud);
        registrarCambio(
                saved,
                null,
                TipoEventoSolicitudDesignacion.APROBACION_SGA,
                "estado",
                EstadoSolicitudDesignacion.PRESENTADA_SGA.name(),
                EstadoSolicitudDesignacion.APROBADA.name(),
                obs
        );
        return toDetalleDto(saved);
    }

    public SolicitudDesignacionDetalleDto observarSolicitudSga(UUID solicitudId, RevisionSgaRequest req) {
        exigirRolSgaOAdmin();
        SolicitudDesignacionJpaEntity solicitud = obtenerSolicitudOCascada(solicitudId);
        if (solicitud.getEstado() != EstadoSolicitudDesignacion.PRESENTADA_SGA) {
            throw new DomainValidationException(
                    "Solo se pueden observar solicitudes en estado PRESENTADA_SGA (estado actual: " + solicitud.getEstado() + ").");
        }
        String obs = req != null ? trimToNull(req.observacionesSga()) : null;
        if (obs == null) {
            throw new DomainValidationException(
                    "Debe indicar el motivo u observaciones de Secretaría de Gestión Académica para devolver la solicitud.");
        }

        Instant ahora = Instant.now();
        solicitud.setEstado(EstadoSolicitudDesignacion.OBSERVADA);
        solicitud.setObservacionesSga(obs);
        solicitud.setRevisadaPorUsuarioId(obtenerUsuarioIdActual());
        solicitud.setFechaRevision(ahora);
        solicitud.setActualizadoEn(ahora);

        SolicitudDesignacionJpaEntity saved = solicitudRepository.save(solicitud);
        registrarCambio(
                saved,
                null,
                TipoEventoSolicitudDesignacion.OBSERVACION_SGA,
                "estado",
                EstadoSolicitudDesignacion.PRESENTADA_SGA.name(),
                EstadoSolicitudDesignacion.OBSERVADA.name(),
                obs
        );
        return toDetalleDto(saved);
    }

    // --- Motor de Clonación del Período Anterior (RF07 / RN11-RN13) ---

    private SolicitudDesignacionJpaEntity resolverSolicitudOrigenClonacion(
            DepartamentoAcademicoJpaEntity depto,
            PeriodoAcademicoJpaEntity periodoDestino,
            UUID solicitudOrigenExplicitaId
    ) {
        int anioDestino = periodoDestino.getAnio() != null ? periodoDestino.getAnio() : Integer.MAX_VALUE;
        if (solicitudOrigenExplicitaId != null) {
            SolicitudDesignacionJpaEntity explicita = obtenerSolicitudOCascada(solicitudOrigenExplicitaId);
            if (!explicita.getDepartamentoAcademico().getId().equals(depto.getId())) {
                throw new DomainValidationException("La solicitud de origen seleccionada pertenece a otro Departamento Académico.");
            }
            if (explicita.getTipoSolicitud() != TipoSolicitudDesignacion.ANUAL) {
                throw new DomainValidationException("Solo se puede clonar una solicitud ANUAL como base del nuevo período (RN11).");
            }
            Integer anioOrigen = explicita.getPeriodoAcademico().getAnio();
            if (explicita.getPeriodoAcademico().getId().equals(periodoDestino.getId())
                    || (anioOrigen != null && anioOrigen >= anioDestino)) {
                throw new DomainValidationException("La solicitud de origen debe corresponder a un período académico anterior.");
            }
            if (explicita.getEstado() != EstadoSolicitudDesignacion.APROBADA
                    && explicita.getEstado() != EstadoSolicitudDesignacion.PRESENTADA_SGA) {
                throw new DomainValidationException("Solo se puede clonar desde una solicitud APROBADA o PRESENTADA_SGA.");
            }
            return explicita;
        }

        List<SolicitudDesignacionJpaEntity> candidatas = solicitudRepository.findCandidatasAnualesOrigenClonacion(
                depto.getId(),
                anioDestino,
                List.of(EstadoSolicitudDesignacion.APROBADA, EstadoSolicitudDesignacion.PRESENTADA_SGA)
        );
        return candidatas.isEmpty() ? null : candidatas.get(0);
    }

    private void ejecutarClonacionDesdeOrigen(
            SolicitudDesignacionJpaEntity destino,
            SolicitudDesignacionJpaEntity origen
    ) {
        List<SolicitudDocenteDesignacionJpaEntity> itemsOrigen = designacionRepository
                .findBySolicitudIdOrderByCreadoEnAsc(origen.getId())
                .stream()
                .filter(i -> i.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO)
                .toList();

        List<UUID> idsOrigen = itemsOrigen.stream().map(SolicitudDocenteDesignacionJpaEntity::getId).toList();
        Map<UUID, List<SolicitudAsignacionCurricularJpaEntity>> asigsOrigenMap = idsOrigen.isEmpty()
                ? Map.of()
                : asignacionRepository.findByDesignacionIdInOrderByCreadoEnAsc(idsOrigen).stream()
                        .collect(Collectors.groupingBy(a -> a.getDesignacion().getId()));

        int designacionesClonadas = 0;
        int asignacionesClonadas = 0;
        int asignacionesOmitidasRN09 = 0;
        UUID deptoDestinoId = destino.getDepartamentoAcademico().getId();

        for (SolicitudDocenteDesignacionJpaEntity itemOrig : itemsOrigen) {
            CuerpoAcademicoMaestroJpaEntity doc = itemOrig.getDocente();
            UUID deptoDocActual = doc.getDepartamentoAcademico() != null ? doc.getDepartamentoAcademico().getId() : null;
            if (Boolean.FALSE.equals(doc.getActivo())
                    || (deptoDocActual != null && !deptoDocActual.equals(deptoDestinoId))
                    || !itemOrig.getCargoDocente().isActivo()
                    || !itemOrig.getDedicacionDocente().isActivo()
                    || !itemOrig.getTipoDesignacion().isActivo()) {
                registrarCambio(
                        destino,
                        null,
                        TipoEventoSolicitudDesignacion.CLONACION_PERIODO_ANTERIOR,
                        "renglon_omitido",
                        resumenDesignacion(itemOrig),
                        null,
                        "Omitido en clonación por docente/catálogo inactivo o cambio de departamento."
                );
                continue;
            }

            int horasItem = itemOrig.getHorasDedicacion() != null ? itemOrig.getHorasDedicacion() : 0;
            int horasYaEnPeriodo = sumarHorasActivasDocenteEnPeriodo(
                    doc.getId(), destino.getPeriodoAcademico().getId(), null);
            if (horasYaEnPeriodo + horasItem > TOPE_HORAS_SEMANALES_DOCENTE) {
                registrarCambio(
                        destino,
                        null,
                        TipoEventoSolicitudDesignacion.CLONACION_PERIODO_ANTERIOR,
                        "tope_50hs_omitido",
                        resumenDesignacion(itemOrig),
                        null,
                        "Omitido en clonación porque superaría las 50 hs semanales del docente en el período destino (RN02)."
                );
                continue;
            }

            SolicitudDocenteDesignacionJpaEntity nuevoItem = new SolicitudDocenteDesignacionJpaEntity();
            nuevoItem.setId(UUID.randomUUID());
            nuevoItem.setSolicitud(destino);
            nuevoItem.setDesignacionOrigenId(itemOrig.getId());
            nuevoItem.setDocente(doc);
            nuevoItem.setCargoDocente(itemOrig.getCargoDocente());
            nuevoItem.setDedicacionDocente(itemOrig.getDedicacionDocente());
            nuevoItem.setTipoDesignacion(itemOrig.getTipoDesignacion());
            nuevoItem.setHorasDedicacion(horasItem);
            nuevoItem.setNroDesignacionMapuche(itemOrig.getNroDesignacionMapuche());
            nuevoItem.setEstadoItem(EstadoItemDesignacion.REPLICADO);
            nuevoItem.setHorasClases(itemOrig.getHorasClases());
            nuevoItem.setHorasInvestigacion(itemOrig.getHorasInvestigacion());
            nuevoItem.setHorasExtension(itemOrig.getHorasExtension());
            nuevoItem.setHorasGestion(itemOrig.getHorasGestion());
            nuevoItem.setHorasDireccionTesis(itemOrig.getHorasDireccionTesis());
            nuevoItem.setHorasOtras(itemOrig.getHorasOtras());
            nuevoItem.setObservaciones(itemOrig.getObservaciones());
            nuevoItem.setCreadoEn(Instant.now());
            nuevoItem.setActualizadoEn(Instant.now());

            SolicitudDocenteDesignacionJpaEntity savedItem = designacionRepository.save(nuevoItem);
            designacionesClonadas++;

            for (SolicitudAsignacionCurricularJpaEntity asigOrig : asigsOrigenMap.getOrDefault(itemOrig.getId(), List.of())) {
                ActividadCurricularPlanJpaEntity act = asigOrig.getActividadCurricular();
                EstadoDictadoActividad estadoAct = act.getEstadoDictado() != null
                        ? act.getEstadoDictado()
                        : EstadoDictadoActividad.ACTIVA;
                ModalidadParticipacionDocente mod = asigOrig.getModalidadParticipacion();

                if (estadoAct.esInactiva()
                        || (estadoAct == EstadoDictadoActividad.SOLO_EXAMENES_FINALES
                                && mod == ModalidadParticipacionDocente.DESARROLLO_CLASES)) {
                    asignacionesOmitidasRN09++;
                    registrarCambio(
                            destino,
                            savedItem.getId(),
                            TipoEventoSolicitudDesignacion.CLONACION_PERIODO_ANTERIOR,
                            "asignatura_omitida_rn09",
                            doc.getApellido() + " — " + act.getNombre() + " [" + mod + "]",
                            null,
                            "Asignatura omitida en clonación por situación actual " + estadoAct + " (RN09)."
                    );
                    continue;
                }

                SolicitudAsignacionCurricularJpaEntity nuevaAsig = new SolicitudAsignacionCurricularJpaEntity();
                nuevaAsig.setId(UUID.randomUUID());
                nuevaAsig.setDesignacion(savedItem);
                nuevaAsig.setCarrera(asigOrig.getCarrera());
                nuevaAsig.setPlanEstudio(asigOrig.getPlanEstudio());
                nuevaAsig.setActividadCurricular(act);
                nuevaAsig.setModalidadParticipacion(mod);
                nuevaAsig.setHorasAsignadas(asigOrig.getHorasAsignadas());
                nuevaAsig.setObservaciones(asigOrig.getObservaciones());
                nuevaAsig.setCreadoEn(Instant.now());
                asignacionRepository.save(nuevaAsig);
                asignacionesClonadas++;
            }
        }

        registrarCambio(
                destino,
                null,
                TipoEventoSolicitudDesignacion.CLONACION_PERIODO_ANTERIOR,
                "solicitud_origen_id",
                origen.getPeriodoAcademico().getDenominacion() + " (" + origen.getEstado() + ")",
                designacionesClonadas + " designación(es) y " + asignacionesClonadas + " asignatura(s) replicadas"
                        + (asignacionesOmitidasRN09 > 0 ? " (" + asignacionesOmitidasRN09 + " omitida(s) por RN09)" : ""),
                "Clonación automática desde período anterior (RF07)"
        );
    }

    // --- Helpers de Reglas de Negocio, Auditoría y Seguridad ---

    private Optional<User> resolverUsuarioAutenticadoSeguro() {
        if (authenticatedUserProvider == null) {
            return Optional.empty();
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                throw new DomainValidationException(
                        "No se pudo verificar la identidad vigente del usuario autenticado en el padrón (RNF01).");
            }
        }
        return userOpt;
    }

    private static void validarEntidadesActivas(
            CuerpoAcademicoMaestroJpaEntity docente,
            CargoDocenteJpaEntity cargo,
            DedicacionDocenteJpaEntity dedicacion,
            TipoDesignacionDocenteJpaEntity tipoDesig
    ) {
        if (Boolean.FALSE.equals(docente.getActivo())) {
            throw new DomainValidationException("El docente seleccionado se encuentra inactivo en el padrón.");
        }
        if (!cargo.isActivo()) {
            throw new DomainValidationException("El cargo docente '" + cargo.getNombre() + "' se encuentra inactivo.");
        }
        if (!dedicacion.isActivo()) {
            throw new DomainValidationException("La dedicación '" + dedicacion.getNombre() + "' se encuentra inactiva.");
        }
        if (!tipoDesig.isActivo()) {
            throw new DomainValidationException("El tipo de designación '" + tipoDesig.getNombre() + "' se encuentra inactivo.");
        }
    }

    private static void validarItemNoExcluido(SolicitudDocenteDesignacionJpaEntity entity) {
        if (entity.getEstadoItem() == EstadoItemDesignacion.EXCLUIDO) {
            throw new DomainValidationException(
                    "La designación se encuentra EXCLUIDA. Debe reactivarla antes de modificar sus datos.");
        }
    }

    private static boolean requiereMotivoAuditoria(
            SolicitudDesignacionJpaEntity solicitud,
            SolicitudDocenteDesignacionJpaEntity entity
    ) {
        return entity.getDesignacionOrigenId() != null
                || entity.getEstadoItem() == EstadoItemDesignacion.REPLICADO
                || solicitud.getEstado() == EstadoSolicitudDesignacion.OBSERVADA;
    }

    private static void exigirMotivoNoVacio(String motivo, String accion) {
        if (motivo == null || motivo.isBlank()) {
            throw new DomainValidationException("Debe indicar el motivo del cambio para " + accion + " (RF08).");
        }
    }

    private void exigirRolSgaOAdmin() {
        Optional<User> userOpt = resolverUsuarioAutenticadoSeguro();
        if (userOpt.isEmpty()) {
            return;
        }
        Set<String> codes = userOpt.get().getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        if (!codes.contains(CodigosRol.AEA) && !codes.contains(CodigosRol.SECRETARIA_ACADEMICA)) {
            throw new DomainValidationException(
                    "Solo Secretaría de Gestión Académica o Administración pueden aprobar u observar solicitudes (RNF01).");
        }
    }

    private UUID obtenerUsuarioIdActual() {
        return resolverUsuarioAutenticadoSeguro().map(User::getId).orElse(null);
    }

    private String obtenerUsuarioNombreActual() {
        String raw = resolverUsuarioAutenticadoSeguro()
                .map(u -> u.getUsername() != null && !u.getUsername().isBlank() ? u.getUsername() : u.getEmail())
                .orElse("Sistema");
        return raw != null && raw.length() > 320 ? raw.substring(0, 320) : raw;
    }

    private void registrarCambio(
            SolicitudDesignacionJpaEntity solicitud,
            UUID designacionId,
            TipoEventoSolicitudDesignacion tipoEvento,
            String campo,
            String valorAnterior,
            String valorNuevo,
            String motivo
    ) {
        if (cambioRepository == null) {
            return;
        }
        SolicitudDesignacionCambioJpaEntity c = new SolicitudDesignacionCambioJpaEntity();
        c.setId(UUID.randomUUID());
        c.setSolicitud(solicitud);
        c.setDesignacionId(designacionId);
        c.setTipoEvento(tipoEvento);
        c.setCampoModificado(campo);
        c.setValorAnterior(valorAnterior);
        c.setValorNuevo(valorNuevo);
        c.setMotivo(motivo);
        c.setUsuarioId(obtenerUsuarioIdActual());
        c.setUsuarioNombre(obtenerUsuarioNombreActual());
        c.setFechaCambio(Instant.now());
        cambioRepository.save(c);
    }

    private static String resumenDesignacion(SolicitudDocenteDesignacionJpaEntity d) {
        String mapuche = d.getNroDesignacionMapuche() != null ? ", Mapuche=" + d.getNroDesignacionMapuche() : "";
        return d.getDocente().getApellido() + ", " + d.getDocente().getNombre()
                + " — " + d.getCargoDocente().getNombre()
                + " (" + d.getDedicacionDocente().getNombre() + " " + d.getHorasDedicacion() + " hs, "
                + d.getTipoDesignacion().getNombre() + mapuche + ")";
    }

    private static String resumenDistribucion(SolicitudDocenteDesignacionJpaEntity d) {
        return "Clases=" + d.getHorasClases()
                + "h, Inv=" + d.getHorasInvestigacion()
                + "h, Ext=" + d.getHorasExtension()
                + "h, Gest=" + d.getHorasGestion()
                + "h, Tesis=" + d.getHorasDireccionTesis()
                + "h, Otras=" + d.getHorasOtras() + "h";
    }

    private void validarCoherenciaRN09(
            ActividadCurricularPlanJpaEntity actividad,
            ModalidadParticipacionDocente modalidad
    ) {
        EstadoDictadoActividad estado = actividad.getEstadoDictado() != null
                ? actividad.getEstadoDictado()
                : EstadoDictadoActividad.ACTIVA;

        if (estado.esInactiva()) {
            throw new DomainValidationException(
                    "No se puede asignar ni reactivar la asignatura '" + actividad.getNombre()
                            + "' porque se encuentra en estado INACTIVA (RN09).");
        }
        if (estado == EstadoDictadoActividad.SOLO_EXAMENES_FINALES
                && modalidad == ModalidadParticipacionDocente.DESARROLLO_CLASES) {
            throw new DomainValidationException(
                    "La asignatura '" + actividad.getNombre()
                            + "' está habilitada únicamente para SOLO EXÁMENES FINALES; no se permite desarrollo de clases (RN09).");
        }
    }

    private void validarTope50Horas(
            CuerpoAcademicoMaestroJpaEntity docente,
            int horasPreviasPeriodo,
            int horasNuevas
    ) {
        int total = horasPreviasPeriodo + horasNuevas;
        if (total > TOPE_HORAS_SEMANALES_DOCENTE) {
            throw new DomainValidationException(
                    "El docente " + docente.getApellido() + ", " + docente.getNombre()
                            + " superaría el tope institucional de " + TOPE_HORAS_SEMANALES_DOCENTE
                            + " hs semanales en el período (acumuladas: " + horasPreviasPeriodo
                            + " hs + nuevas: " + horasNuevas + " hs = " + total + " hs) (RN02).");
        }
    }

    private int sumarHorasActivasDocenteEnPeriodo(UUID docenteId, UUID periodoId, UUID excluirDesignacionId) {
        List<SolicitudDocenteDesignacionJpaEntity> activas =
                designacionRepository.findActivasByDocenteAndPeriodo(docenteId, periodoId);
        int sum = 0;
        for (SolicitudDocenteDesignacionJpaEntity d : activas) {
            if (excluirDesignacionId != null && excluirDesignacionId.equals(d.getId())) {
                continue;
            }
            if (d.getHorasDedicacion() != null) {
                sum += d.getHorasDedicacion();
            }
        }
        return sum;
    }

    private SolicitudDesignacionJpaEntity obtenerSolicitudOCascada(UUID solicitudId) {
        return solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la solicitud de designación ID: " + solicitudId));
    }

    private SolicitudDocenteDesignacionJpaEntity obtenerDesignacionEnSolicitud(UUID solicitudId, UUID designacionId) {
        SolicitudDocenteDesignacionJpaEntity entity = designacionRepository.findById(designacionId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la designación docente ID: " + designacionId));
        if (!entity.getSolicitud().getId().equals(solicitudId)) {
            throw new ResourceNotFoundException("La designación no pertenece a la solicitud indicada.");
        }
        return entity;
    }

    private void validarDocenteEnDepartamentoSolicitud(
            CuerpoAcademicoMaestroJpaEntity docente,
            SolicitudDesignacionJpaEntity solicitud
    ) {
        Optional<User> userOpt = resolverUsuarioAutenticadoSeguro();
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean esAdminOSga = codes.contains(CodigosRol.AEA) || codes.contains(CodigosRol.SECRETARIA_ACADEMICA);
        if (!esAdminOSga && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID deptoDocente = docente.getDepartamentoAcademico() != null
                    ? docente.getDepartamentoAcademico().getId()
                    : null;
            UUID deptoSolicitud = solicitud.getDepartamentoAcademico().getId();
            if (deptoDocente == null || !deptoDocente.equals(deptoSolicitud)) {
                throw new DomainValidationException(
                        "Un Director de Departamento solo puede designar docentes pertenecientes a su propio Departamento Académico (RNF01).");
            }
        }
    }

    private void validarEscrituraSobreSolicitud(SolicitudDesignacionJpaEntity solicitud) {
        validarAccesoEscrituraDepartamento(solicitud.getDepartamentoAcademico().getId());
        if (!esEstadoEditable(solicitud.getEstado())) {
            throw new DomainValidationException(
                    "La solicitud se encuentra en estado " + solicitud.getEstado()
                            + " y no admite modificaciones. Solo se pueden editar solicitudes en estado BORRADOR u OBSERVADA.");
        }
    }

    private static boolean esEstadoEditable(EstadoSolicitudDesignacion estado) {
        return estado != null && estado.esEditablePorDepartamento();
    }

    private boolean esEditablePorUsuarioActual(SolicitudDesignacionJpaEntity solicitud) {
        if (!esEstadoEditable(solicitud.getEstado())) {
            return false;
        }
        Optional<User> userOpt = resolverUsuarioAutenticadoSeguro();
        if (userOpt.isEmpty()) {
            return true;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        if (codes.contains(CodigosRol.AEA) || codes.contains(CodigosRol.SECRETARIA_ACADEMICA)) {
            return true;
        }
        if (codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            return miDepto != null && miDepto.equals(solicitud.getDepartamentoAcademico().getId());
        }
        return false;
    }

    private UUID resolverDepartamentoLectura(UUID requestedDeptoId) {
        Optional<User> userOpt = resolverUsuarioAutenticadoSeguro();
        if (userOpt.isEmpty()) {
            return requestedDeptoId;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean tieneVisionInstitucional = codes.stream().anyMatch(ROLES_VISION_INSTITUCIONAL::contains);
        if (!tieneVisionInstitucional && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            if (miDepto == null) {
                throw new DomainValidationException(
                        "Su usuario Director de Departamento no tiene un Departamento Académico asignado.");
            }
            return miDepto;
        }
        return requestedDeptoId;
    }

    private void validarAccesoLecturaDepartamento(UUID deptoSolicitudId) {
        Optional<User> userOpt = resolverUsuarioAutenticadoSeguro();
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean tieneVisionInstitucional = codes.stream().anyMatch(ROLES_VISION_INSTITUCIONAL::contains);
        if (!tieneVisionInstitucional && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            if (miDepto == null || !miDepto.equals(deptoSolicitudId)) {
                throw new DomainValidationException(
                        "Un Director de Departamento solo puede consultar solicitudes de su propio Departamento Académico (RNF01).");
            }
        }
    }

    private UUID resolverDepartamentoEscritura(UUID requestedDeptoId) {
        Optional<User> userOpt = resolverUsuarioAutenticadoSeguro();
        if (userOpt.isEmpty()) {
            return requestedDeptoId;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean esAdminOSga = codes.contains(CodigosRol.AEA) || codes.contains(CodigosRol.SECRETARIA_ACADEMICA);
        if (!esAdminOSga && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            if (miDepto == null) {
                throw new DomainValidationException(
                        "Su usuario Director de Departamento no tiene un Departamento Académico asignado.");
            }
            if (requestedDeptoId != null && !miDepto.equals(requestedDeptoId)) {
                throw new DomainValidationException(
                        "Un Director de Departamento solo puede gestionar solicitudes de su propio Departamento Académico (RNF01).");
            }
            return miDepto;
        }
        return requestedDeptoId;
    }

    private void validarAccesoEscrituraDepartamento(UUID deptoSolicitudId) {
        Optional<User> userOpt = resolverUsuarioAutenticadoSeguro();
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        Set<String> codes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        boolean esAdminOSga = codes.contains(CodigosRol.AEA) || codes.contains(CodigosRol.SECRETARIA_ACADEMICA);
        if (!esAdminOSga && codes.contains(CodigosRol.DIRECTOR_DEPARTAMENTO)) {
            UUID miDepto = user.getDepartamentoAcademicoId();
            if (miDepto == null || !miDepto.equals(deptoSolicitudId)) {
                throw new DomainValidationException(
                        "Un Director de Departamento solo puede modificar solicitudes de su propio Departamento Académico (RNF01).");
            }
        }
    }

    private static int validarHoraNoNegativa(Integer valor, String etiqueta) {
        int v = valor != null ? valor : 0;
        if (v < 0) {
            throw new DomainValidationException("Las horas de '" + etiqueta + "' no pueden ser negativas.");
        }
        return v;
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    // --- Mappers a DTOs inmutables dentro de @Transactional (OSIV=false safe) ---

    private List<CambioSolicitudDesignacionDto> obtenerHistorialDtos(UUID solicitudId) {
        if (cambioRepository == null) {
            return List.of();
        }
        return cambioRepository.findBySolicitudIdOrderByFechaCambioDesc(solicitudId)
                .stream()
                .map(c -> new CambioSolicitudDesignacionDto(
                        c.getId(),
                        c.getSolicitud().getId(),
                        c.getDesignacionId(),
                        c.getTipoEvento(),
                        c.getCampoModificado(),
                        c.getValorAnterior(),
                        c.getValorNuevo(),
                        c.getMotivo(),
                        c.getUsuarioId(),
                        c.getUsuarioNombre(),
                        c.getFechaCambio()
                ))
                .toList();
    }

    private SolicitudDesignacionResumenDto toResumenDto(SolicitudDesignacionJpaEntity s) {
        List<SolicitudDocenteDesignacionJpaEntity> items =
                designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(s.getId());
        long activas = items.stream().filter(i -> i.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO).count();
        long conAlerta = items.stream()
                .filter(i -> i.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO)
                .filter(i -> i.totalHorasDistribuidas() < (i.getHorasDedicacion() != null ? i.getHorasDedicacion() : 0))
                .count();

        DepartamentoAcademicoJpaEntity depto = s.getDepartamentoAcademico();
        PeriodoAcademicoJpaEntity periodo = s.getPeriodoAcademico();
        return new SolicitudDesignacionResumenDto(
                s.getId(),
                depto.getId(),
                depto.getCodigo(),
                depto.getDenominacion(),
                periodo.getId(),
                periodo.getCodigo(),
                periodo.getDenominacion(),
                periodo.getAnio(),
                s.getTipoSolicitud(),
                s.getEstado(),
                esEditablePorUsuarioActual(s),
                activas,
                conAlerta,
                s.getObservacionesDepartamento(),
                s.getObservacionesSga(),
                s.getFechaEmision(),
                s.getCreadoEn(),
                s.getActualizadoEn()
        );
    }

    private SolicitudDesignacionDetalleDto toDetalleDto(SolicitudDesignacionJpaEntity s) {
        List<SolicitudDocenteDesignacionJpaEntity> items =
                designacionRepository.findBySolicitudIdOrderByCreadoEnAsc(s.getId());

        List<UUID> itemIds = items.stream().map(SolicitudDocenteDesignacionJpaEntity::getId).toList();
        Map<UUID, List<SolicitudAsignacionCurricularJpaEntity>> asigsPorDesignacion = itemIds.isEmpty()
                ? Map.of()
                : asignacionRepository.findByDesignacionIdInOrderByCreadoEnAsc(itemIds).stream()
                        .collect(Collectors.groupingBy(a -> a.getDesignacion().getId()));

        UUID periodoId = s.getPeriodoAcademico().getId();
        Map<UUID, Integer> totalHorasPorDocente = new HashMap<>();
        for (SolicitudDocenteDesignacionJpaEntity item : items) {
            UUID docId = item.getDocente().getId();
            totalHorasPorDocente.computeIfAbsent(docId,
                    id -> sumarHorasActivasDocenteEnPeriodo(id, periodoId, null));
        }

        List<DesignacionDocenteItemDto> designacionDtos = items.stream()
                .map(i -> toDesignacionDto(
                        i,
                        asigsPorDesignacion.getOrDefault(i.getId(), List.of()),
                        totalHorasPorDocente.getOrDefault(i.getDocente().getId(), 0)))
                .toList();

        DepartamentoAcademicoJpaEntity depto = s.getDepartamentoAcademico();
        PeriodoAcademicoJpaEntity periodo = s.getPeriodoAcademico();

        return new SolicitudDesignacionDetalleDto(
                s.getId(),
                s.getSolicitudOrigenId(),
                depto.getId(),
                depto.getCodigo(),
                depto.getDenominacion(),
                periodo.getId(),
                periodo.getCodigo(),
                periodo.getDenominacion(),
                periodo.getAnio(),
                s.getTipoSolicitud(),
                s.getEstado(),
                esEditablePorUsuarioActual(s),
                s.getObservacionesDepartamento(),
                s.getObservacionesSga(),
                s.getFechaEmision(),
                s.getCreadoEn(),
                s.getActualizadoEn(),
                designacionDtos,
                obtenerHistorialDtos(s.getId())
        );
    }

    private DesignacionDocenteItemDto toDesignacionDto(
            SolicitudDocenteDesignacionJpaEntity d,
            List<SolicitudAsignacionCurricularJpaEntity> asignaciones,
            int totalHorasDocentePeriodo
    ) {
        int dedHoras = d.getHorasDedicacion() != null ? d.getHorasDedicacion() : 0;
        int distribuidas = d.totalHorasDistribuidas();
        int sinDistribuir = Math.max(0, dedHoras - distribuidas);
        boolean alerta = d.getEstadoItem() != EstadoItemDesignacion.EXCLUIDO && sinDistribuir > 0;

        CuerpoAcademicoMaestroJpaEntity doc = d.getDocente();
        CargoDocenteJpaEntity cargo = d.getCargoDocente();
        DedicacionDocenteJpaEntity ded = d.getDedicacionDocente();
        TipoDesignacionDocenteJpaEntity tipo = d.getTipoDesignacion();

        return new DesignacionDocenteItemDto(
                d.getId(),
                d.getSolicitud().getId(),
                d.getDesignacionOrigenId(),
                doc.getId(),
                doc.getDni(),
                doc.getLegajo(),
                doc.getApellido(),
                doc.getNombre(),
                cargo.getId(),
                cargo.getCodigo(),
                cargo.getNombre(),
                ded.getId(),
                ded.getCodigo(),
                ded.getNombre(),
                dedHoras,
                tipo.getId(),
                tipo.getCodigo(),
                tipo.getNombre(),
                d.getNroDesignacionMapuche(),
                d.getEstadoItem(),
                d.getHorasClases(),
                d.getHorasInvestigacion(),
                d.getHorasExtension(),
                d.getHorasGestion(),
                d.getHorasDireccionTesis(),
                d.getHorasOtras(),
                distribuidas,
                sinDistribuir,
                alerta,
                totalHorasDocentePeriodo,
                d.getObservaciones(),
                asignaciones.stream().map(this::toAsignacionDto).toList()
        );
    }

    private AsignacionCurricularPeriodoDto toAsignacionDto(SolicitudAsignacionCurricularJpaEntity a) {
        CarreraJpaEntity carrera = a.getCarrera();
        PlanEstudioFt001JpaEntity plan = a.getPlanEstudio();
        ActividadCurricularPlanJpaEntity act = a.getActividadCurricular();
        return new AsignacionCurricularPeriodoDto(
                a.getId(),
                carrera.getId(),
                carrera.getNombre(),
                carrera.getSigla(),
                plan.getId(),
                plan.getNombrePlan(),
                plan.getEstadoVigencia(),
                act.getId(),
                act.getCodigo(),
                act.getNombre(),
                act.getEstadoDictado(),
                act.getHorasSemanales(),
                a.getModalidadParticipacion(),
                a.getHorasAsignadas(),
                a.getObservaciones()
        );
    }
}
