package com.undec.acreditacion.infrastructure.estandares.service;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.ModoSeccion311;
import com.undec.acreditacion.domain.entities.ModoSeccion312;
import com.undec.acreditacion.domain.entities.TipoEvaluacionEstandar22;
import com.undec.acreditacion.domain.entities.TipoSeccionCargaHoraria;
import com.undec.acreditacion.domain.entities.TipoSeccionEstandar22;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.estandares.dto.CambiarVigenciaEstandarRequest;
import com.undec.acreditacion.infrastructure.estandares.dto.EstandarDetalleDto;
import com.undec.acreditacion.infrastructure.estandares.dto.EstandarResumenDto;
import com.undec.acreditacion.infrastructure.estandares.dto.GuardarEstandarRequest;
import com.undec.acreditacion.infrastructure.estandares.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class EstandarService {

    private static final Pattern VERSION_SUFFIX_PATTERN = Pattern.compile("^(.*)\\s+\\(v(\\d+)\\)$");

    private final SpringDataEstandarRepository estandarRepository;
    private final SpringDataEstandarEscalaEvaluacionRepository escalaRepository;
    private final SpringDataEstandarGrupo22Repository grupoRepository;
    private final SpringDataEstandarItem22Repository itemRepository;
    private final SpringDataEstandarAreaCargaHorariaRepository areaRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final SpringDataPlanEstudioFt001Repository planRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public EstandarService(
            SpringDataEstandarRepository estandarRepository,
            SpringDataEstandarEscalaEvaluacionRepository escalaRepository,
            SpringDataEstandarGrupo22Repository grupoRepository,
            SpringDataEstandarItem22Repository itemRepository,
            SpringDataEstandarAreaCargaHorariaRepository areaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataPlanEstudioFt001Repository planRepository
    ) {
        this(estandarRepository, escalaRepository, grupoRepository, itemRepository,
                areaRepository, carreraRepository, planRepository, null);
    }

    @Autowired
    public EstandarService(
            SpringDataEstandarRepository estandarRepository,
            SpringDataEstandarEscalaEvaluacionRepository escalaRepository,
            SpringDataEstandarGrupo22Repository grupoRepository,
            SpringDataEstandarItem22Repository itemRepository,
            SpringDataEstandarAreaCargaHorariaRepository areaRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataPlanEstudioFt001Repository planRepository,
            AuthenticatedUserProvider authenticatedUserProvider
    ) {
        this.estandarRepository = Objects.requireNonNull(estandarRepository);
        this.escalaRepository = Objects.requireNonNull(escalaRepository);
        this.grupoRepository = Objects.requireNonNull(grupoRepository);
        this.itemRepository = Objects.requireNonNull(itemRepository);
        this.areaRepository = Objects.requireNonNull(areaRepository);
        this.carreraRepository = Objects.requireNonNull(carreraRepository);
        this.planRepository = Objects.requireNonNull(planRepository);
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    @Transactional(readOnly = true)
    public List<EstandarResumenDto> listar(UUID carreraId, EstadoVigenciaEstandar estadoVigencia) {
        List<EstandarJpaEntity> entidades;
        if (carreraId != null) {
            entidades = estandarRepository.findByCarreraIdOrderByActualizadoEnDesc(carreraId);
            if (estadoVigencia != null) {
                entidades = entidades.stream()
                        .filter(e -> e.getEstadoVigencia() == estadoVigencia)
                        .toList();
            }
        } else if (estadoVigencia != null) {
            entidades = estandarRepository.findByEstadoVigenciaOrderByActualizadoEnDesc(estadoVigencia);
        } else {
            entidades = estandarRepository.findAllByOrderByActualizadoEnDesc();
        }

        UUID uaDirectorFiltro = resolverUaSiSoloDirectorEscuela();
        if (uaDirectorFiltro != null) {
            entidades = entidades.stream()
                    .filter(e -> e.getCarrera() != null
                            && e.getCarrera().getUnidadAcademica() != null
                            && uaDirectorFiltro.equals(e.getCarrera().getUnidadAcademica().getId()))
                    .toList();
        }

        return entidades.stream().map(this::toResumenDto).toList();
    }

    @Transactional(readOnly = true)
    public EstandarDetalleDto obtenerDetalle(UUID id) {
        EstandarJpaEntity estandar = estandarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el estándar con ID: " + id));
        return toDetalleDto(estandar);
    }

    @Transactional(readOnly = true)
    public EstandarDetalleDto obtenerVigentePorCarrera(UUID carreraId) {
        if (carreraId == null) {
            throw new DomainValidationException("El ID de la carrera es obligatorio.");
        }
        EstandarJpaEntity estandar = estandarRepository
                .findByCarreraIdAndEstadoVigencia(carreraId, EstadoVigenciaEstandar.VIGENTE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La carrera indicada no posee un estándar vigente configurado."));
        return toDetalleDto(estandar);
    }

    public EstandarDetalleDto crear(GuardarEstandarRequest req) {
        validarRequest(req, true);
        CarreraJpaEntity carrera = carreraRepository.findById(req.carreraId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la carrera ID: " + req.carreraId()));
        validarPermisoGestionCarrera(carrera);

        EstadoVigenciaEstandar estadoInicial = req.estadoVigencia() != null
                ? req.estadoVigencia()
                : EstadoVigenciaEstandar.VIGENTE;

        if (estadoInicial == EstadoVigenciaEstandar.VIGENTE) {
            degradarVigenteAnterior(carrera.getId(), null);
        }

        Instant now = Instant.now();
        EstandarJpaEntity estandar = new EstandarJpaEntity();
        estandar.setId(UUID.randomUUID());
        estandar.setCarrera(carrera);
        estandar.setEstadoVigencia(estadoInicial);
        estandar.setCreadoPor(authenticatedUserProvider != null
                ? authenticatedUserProvider.getAuthenticatedUserIdOrNull()
                : null);
        estandar.setCreadoEn(now);
        estandar.setActualizadoEn(now);
        aplicarCabecera(estandar, req);

        EstandarJpaEntity guardado = estandarRepository.save(estandar);
        guardarEstructuraHija(guardado, req);
        return toDetalleDto(guardado);
    }

    public EstandarDetalleDto actualizar(UUID id, GuardarEstandarRequest req) {
        EstandarJpaEntity estandar = estandarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el estándar ID: " + id));
        validarPermisoGestionCarrera(estandar.getCarrera());

        if (estandar.getEstadoVigencia() == EstadoVigenciaEstandar.NO_VIGENTE) {
            throw new DomainValidationException(
                    "No se puede editar un estándar NO VIGENTE. Cree una nueva versión si requiere cambios normativos.");
        }
        long planesAsociados = planRepository.countByEstandarId(id);
        if (planesAsociados > 0) {
            throw new DomainValidationException(
                    "Este estándar ya tiene " + planesAsociados + " plan(es) de estudio asociados y no puede modificarse. Cree una nueva versión.");
        }

        validarRequest(req, false);
        aplicarCabecera(estandar, req);
        estandar.setActualizadoEn(Instant.now());
        EstandarJpaEntity guardado = estandarRepository.save(estandar);

        escalaRepository.deleteByEstandarId(id);
        grupoRepository.deleteByEstandarId(id);
        areaRepository.deleteByEstandarId(id);
        escalaRepository.flush();
        grupoRepository.flush();
        areaRepository.flush();

        guardarEstructuraHija(guardado, req);
        return toDetalleDto(guardado);
    }

    public EstandarDetalleDto cambiarVigencia(UUID id, CambiarVigenciaEstandarRequest req) {
        if (req == null || req.estadoVigencia() == null) {
            throw new DomainValidationException("El nuevo estado de vigencia del estándar es obligatorio.");
        }
        EstandarJpaEntity estandar = estandarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el estándar ID: " + id));
        validarPermisoGestionCarrera(estandar.getCarrera());

        if (req.estadoVigencia() == EstadoVigenciaEstandar.VIGENTE
                && estandar.getCarrera() != null) {
            degradarVigenteAnterior(estandar.getCarrera().getId(), id);
        }

        estandar.setEstadoVigencia(req.estadoVigencia());
        estandar.setActualizadoEn(Instant.now());
        return toDetalleDto(estandarRepository.save(estandar));
    }

    public EstandarDetalleDto crearNuevaVersion(UUID id) {
        EstandarJpaEntity origen = estandarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el estándar ID: " + id));
        validarPermisoGestionCarrera(origen.getCarrera());

        EstandarDetalleDto detalleOrigen = toDetalleDto(origen);
        if (origen.getCarrera() != null) {
            degradarVigenteAnterior(origen.getCarrera().getId(), null);
        }

        Instant now = Instant.now();
        EstandarJpaEntity nuevo = new EstandarJpaEntity();
        nuevo.setId(UUID.randomUUID());
        nuevo.setCarrera(origen.getCarrera());
        nuevo.setNombre(siguienteNombreVersion(origen.getNombre()));
        nuevo.setResolucionMinisterial(origen.getResolucionMinisterial());
        nuevo.setAnioVigencia(origen.getAnioVigencia());
        nuevo.setEstadoVigencia(EstadoVigenciaEstandar.VIGENTE);
        nuevo.setTipoSeccion22(origen.getTipoSeccion22());
        nuevo.setTipoEvaluacion22(origen.getTipoEvaluacion22());
        nuevo.setInstrucciones22(origen.getInstrucciones22());
        nuevo.setObservaciones(origen.getObservaciones());
        nuevo.setHabilitar311(origen.getHabilitar311());
        nuevo.setModo311(origen.getModo311());
        nuevo.setHorasMinimasTotal311(origen.getHorasMinimasTotal311());
        nuevo.setHabilitar312(origen.getHabilitar312());
        nuevo.setModo312(origen.getModo312());
        nuevo.setHorasMinimasTotal312(origen.getHorasMinimasTotal312());
        nuevo.setCreadoPor(authenticatedUserProvider != null
                ? authenticatedUserProvider.getAuthenticatedUserIdOrNull()
                : null);
        nuevo.setCreadoEn(now);
        nuevo.setActualizadoEn(now);

        EstandarJpaEntity guardado = estandarRepository.save(nuevo);

        GuardarEstandarRequest reqClonado = new GuardarEstandarRequest(
                origen.getCarrera() != null ? origen.getCarrera().getId() : null,
                guardado.getNombre(),
                guardado.getResolucionMinisterial(),
                guardado.getAnioVigencia(),
                EstadoVigenciaEstandar.VIGENTE,
                guardado.getTipoSeccion22(),
                guardado.getTipoEvaluacion22(),
                guardado.getInstrucciones22(),
                guardado.getObservaciones(),
                guardado.getHabilitar311(),
                guardado.getModo311(),
                guardado.getHorasMinimasTotal311(),
                guardado.getHabilitar312(),
                guardado.getModo312(),
                guardado.getHorasMinimasTotal312(),
                detalleOrigen.escalas().stream()
                        .map(e -> new GuardarEstandarRequest.EscalaItemRequest(
                                e.orden(), e.codigo(), e.etiqueta(), e.descripcionReferencia(), e.esValorPorDefecto()))
                        .toList(),
                detalleOrigen.grupos22().stream()
                        .map(g -> new GuardarEstandarRequest.Grupo22Request(
                                g.orden(),
                                g.codigo(),
                                g.titulo(),
                                g.descripcion(),
                                g.habilitado(),
                                g.items().stream()
                                        .map(i -> new GuardarEstandarRequest.Item22Request(i.orden(), i.enunciado()))
                                        .toList()))
                        .toList(),
                detalleOrigen.areasCargaHoraria().stream()
                        .map(a -> new GuardarEstandarRequest.AreaCargaHorariaRequest(
                                a.tipoSeccion(), a.orden(), a.codigo(), a.nombre(), a.etiquetaPractica(), a.habilitada(), a.horasMinimasCarrera()))
                        .toList()
        );

        guardarEstructuraHija(guardado, reqClonado);
        return toDetalleDto(guardado);
    }

    public EstandarDetalleDto toDetalleDto(EstandarJpaEntity estandar) {
        UUID id = estandar.getId();
        List<EstandarEscalaEvaluacionJpaEntity> escalasEnt =
                orEmpty(escalaRepository.findByEstandarIdOrderByOrdenAsc(id));
        List<EstandarGrupo22JpaEntity> gruposEnt =
                orEmpty(grupoRepository.findByEstandarIdOrderByOrdenAsc(id));
        List<EstandarAreaCargaHorariaJpaEntity> areasEnt =
                orEmpty(areaRepository.findByEstandarIdOrderByOrdenAsc(id));

        Map<UUID, List<EstandarItem22JpaEntity>> itemsPorGrupo = Map.of();
        if (!gruposEnt.isEmpty()) {
            List<UUID> grupoIds = gruposEnt.stream().map(EstandarGrupo22JpaEntity::getId).toList();
            List<EstandarItem22JpaEntity> items = orEmpty(itemRepository.findByGrupoIdInOrderByOrdenAsc(grupoIds));
            itemsPorGrupo = items.stream()
                    .filter(i -> i.getGrupo() != null && i.getGrupo().getId() != null)
                    .collect(Collectors.groupingBy(i -> i.getGrupo().getId()));
        }

        long planesAsociados = planRepository.countByEstandarId(id);
        boolean editable = estandar.getEstadoVigencia() == EstadoVigenciaEstandar.VIGENTE && planesAsociados == 0;

        List<EstandarDetalleDto.EscalaDto> escalas = escalasEnt.stream()
                .map(e -> new EstandarDetalleDto.EscalaDto(
                        e.getId(),
                        e.getOrden(),
                        e.getCodigo(),
                        e.getEtiqueta(),
                        e.getDescripcionReferencia(),
                        Boolean.TRUE.equals(e.getEsValorPorDefecto())))
                .toList();

        Map<UUID, List<EstandarItem22JpaEntity>> finalItemsPorGrupo = itemsPorGrupo;
        List<EstandarDetalleDto.Grupo22Dto> grupos = gruposEnt.stream()
                .map(g -> {
                    List<EstandarDetalleDto.Item22Dto> items = finalItemsPorGrupo
                            .getOrDefault(g.getId(), List.of()).stream()
                            .map(it -> new EstandarDetalleDto.Item22Dto(it.getId(), it.getOrden(), it.getEnunciado()))
                            .toList();
                    return new EstandarDetalleDto.Grupo22Dto(
                            g.getId(),
                            g.getOrden(),
                            g.getCodigo(),
                            g.getTitulo(),
                            g.getDescripcion(),
                            Boolean.TRUE.equals(g.getHabilitado()),
                            items
                    );
                })
                .toList();

        List<EstandarDetalleDto.AreaCargaHorariaDto> areas = areasEnt.stream()
                .map(a -> new EstandarDetalleDto.AreaCargaHorariaDto(
                        a.getId(),
                        a.getTipoSeccion() != null ? a.getTipoSeccion() : TipoSeccionCargaHoraria.CARGA_TOTAL_311,
                        a.getOrden(),
                        a.getCodigo(),
                        a.getNombre(),
                        a.getEtiquetaPractica(),
                        Boolean.TRUE.equals(a.getHabilitada()),
                        a.getHorasMinimasCarrera()))
                .toList();

        boolean hab311 = estandar.getHabilitar311() == null || Boolean.TRUE.equals(estandar.getHabilitar311());
        ModoSeccion311 m311 = estandar.getModo311() != null
                ? estandar.getModo311()
                : (estandar.getTipoSeccion22() == TipoSeccionEstandar22.EJES
                        ? ModoSeccion311.BLOQUES_CURRICULARES
                        : ModoSeccion311.AREAS_TEMATICAS);
        boolean hab312 = estandar.getHabilitar312() == null || Boolean.TRUE.equals(estandar.getHabilitar312());
        ModoSeccion312 m312 = estandar.getModo312() != null
                ? estandar.getModo312()
                : (estandar.getTipoSeccion22() == TipoSeccionEstandar22.EJES
                        ? ModoSeccion312.MODALIDADES_PRACTICAS
                        : ModoSeccion312.AREAS_PRACTICAS);

        return new EstandarDetalleDto(
                estandar.getId(),
                estandar.getCarrera() != null ? estandar.getCarrera().getId() : null,
                estandar.getCarrera() != null ? estandar.getCarrera().getNombre() : null,
                estandar.getCarrera() != null ? estandar.getCarrera().getSigla() : null,
                estandar.getNombre(),
                estandar.getResolucionMinisterial(),
                estandar.getAnioVigencia(),
                estandar.getEstadoVigencia(),
                estandar.getTipoSeccion22(),
                estandar.getTipoEvaluacion22(),
                estandar.getInstrucciones22(),
                estandar.getObservaciones(),
                hab311,
                m311,
                estandar.getHorasMinimasTotal311(),
                hab312,
                m312,
                estandar.getHorasMinimasTotal312(),
                planesAsociados,
                editable,
                escalas,
                grupos,
                areas,
                estandar.getCreadoEn(),
                estandar.getActualizadoEn()
        );
    }

    private EstandarResumenDto toResumenDto(EstandarJpaEntity estandar) {
        UUID id = estandar.getId();
        List<EstandarGrupo22JpaEntity> grupos = orEmpty(grupoRepository.findByEstandarIdOrderByOrdenAsc(id));
        int gruposHabilitados = (int) grupos.stream().filter(g -> Boolean.TRUE.equals(g.getHabilitado())).count();
        int totalItems = 0;
        if (!grupos.isEmpty()) {
            Set<UUID> habilitadosIds = grupos.stream()
                    .filter(g -> Boolean.TRUE.equals(g.getHabilitado()))
                    .map(EstandarGrupo22JpaEntity::getId)
                    .collect(Collectors.toSet());
            if (!habilitadosIds.isEmpty()) {
                totalItems = orEmpty(itemRepository.findByGrupoIdInOrderByOrdenAsc(habilitadosIds)).size();
            }
        }
        List<EstandarAreaCargaHorariaJpaEntity> areasAll = orEmpty(areaRepository.findByEstandarIdOrderByOrdenAsc(id));
        int areasHabilitadas = (int) areasAll.stream()
                .filter(a -> Boolean.TRUE.equals(a.getHabilitada()))
                .filter(a -> a.getTipoSeccion() == null || a.getTipoSeccion() == TipoSeccionCargaHoraria.CARGA_TOTAL_311)
                .count();
        long planesAsociados = planRepository.countByEstandarId(id);
        boolean editable = estandar.getEstadoVigencia() == EstadoVigenciaEstandar.VIGENTE && planesAsociados == 0;

        return new EstandarResumenDto(
                estandar.getId(),
                estandar.getCarrera() != null ? estandar.getCarrera().getId() : null,
                estandar.getCarrera() != null ? estandar.getCarrera().getNombre() : null,
                estandar.getCarrera() != null ? estandar.getCarrera().getSigla() : null,
                estandar.getNombre(),
                estandar.getResolucionMinisterial(),
                estandar.getAnioVigencia(),
                estandar.getEstadoVigencia(),
                estandar.getTipoSeccion22(),
                estandar.getTipoEvaluacion22(),
                gruposHabilitados,
                totalItems,
                areasHabilitadas,
                planesAsociados,
                editable,
                estandar.getCreadoEn(),
                estandar.getActualizadoEn()
        );
    }

    private void degradarVigenteAnterior(UUID carreraId, UUID excluirId) {
        estandarRepository.findByCarreraIdAndEstadoVigencia(carreraId, EstadoVigenciaEstandar.VIGENTE)
                .ifPresent(anterior -> {
                    if (!anterior.getId().equals(excluirId)) {
                        anterior.setEstadoVigencia(EstadoVigenciaEstandar.NO_VIGENTE);
                        anterior.setActualizadoEn(Instant.now());
                        estandarRepository.save(anterior);
                        estandarRepository.flush();
                    }
                });
    }

    private void aplicarCabecera(EstandarJpaEntity estandar, GuardarEstandarRequest req) {
        estandar.setNombre(req.nombre().trim());
        estandar.setResolucionMinisterial(req.resolucionMinisterial() != null ? req.resolucionMinisterial().trim() : null);
        estandar.setAnioVigencia(req.anioVigencia());
        TipoSeccionEstandar22 tipo22 = resolverTipoSeccion22(req);
        TipoEvaluacionEstandar22 eval22 = resolverTipoEvaluacion22(req, tipo22);
        estandar.setTipoSeccion22(tipo22);
        estandar.setTipoEvaluacion22(eval22);
        estandar.setInstrucciones22(req.instrucciones22() != null ? req.instrucciones22().trim() : null);
        estandar.setObservaciones(req.observaciones() != null ? req.observaciones().trim() : null);

        boolean hab311 = req.habilitar311() == null || Boolean.TRUE.equals(req.habilitar311());
        ModoSeccion311 m311 = req.modo311() != null
                ? req.modo311()
                : (tipo22 == TipoSeccionEstandar22.EJES
                        ? ModoSeccion311.BLOQUES_CURRICULARES
                        : ModoSeccion311.AREAS_TEMATICAS);
        boolean hab312 = req.habilitar312() == null || Boolean.TRUE.equals(req.habilitar312());
        ModoSeccion312 m312 = req.modo312() != null
                ? req.modo312()
                : (tipo22 == TipoSeccionEstandar22.EJES
                        ? ModoSeccion312.MODALIDADES_PRACTICAS
                        : ModoSeccion312.AREAS_PRACTICAS);

        estandar.setHabilitar311(hab311);
        estandar.setModo311(m311);
        estandar.setHorasMinimasTotal311(hab311 ? normalizarHorasMinimas(req.horasMinimasTotal311()) : null);
        estandar.setHabilitar312(hab312);
        estandar.setModo312(m312);
        estandar.setHorasMinimasTotal312(hab312 ? normalizarHorasMinimas(req.horasMinimasTotal312()) : null);
    }

    private void guardarEstructuraHija(EstandarJpaEntity estandar, GuardarEstandarRequest req) {
        TipoSeccionEstandar22 tipo22 = estandar.getTipoSeccion22();

        if (tipo22 != TipoSeccionEstandar22.NINGUNA) {
            Set<String> codigosEscala = new HashSet<>();
            int ordenEscala = 1;
            List<EstandarEscalaEvaluacionJpaEntity> escalasAGuardar = new ArrayList<>();
            for (GuardarEstandarRequest.EscalaItemRequest eReq : req.escalas()) {
                if (eReq.etiqueta() == null || eReq.etiqueta().isBlank()) {
                    continue;
                }
                String baseCod = eReq.codigo() != null && !eReq.codigo().isBlank()
                        ? normalizarCodigo(eReq.codigo())
                        : normalizarCodigo(eReq.etiqueta());
                String cod = hacerCodigoUnico(baseCod, codigosEscala);

                EstandarEscalaEvaluacionJpaEntity esc = new EstandarEscalaEvaluacionJpaEntity();
                esc.setId(UUID.randomUUID());
                esc.setEstandar(estandar);
                esc.setOrden(eReq.orden() != null ? eReq.orden() : ordenEscala);
                esc.setCodigo(cod);
                esc.setEtiqueta(eReq.etiqueta().trim());
                esc.setDescripcionReferencia(eReq.descripcionReferencia() != null ? eReq.descripcionReferencia().trim() : null);
                esc.setEsValorPorDefecto(Boolean.TRUE.equals(eReq.esValorPorDefecto()));
                escalasAGuardar.add(esc);
                ordenEscala++;
            }
            if (!escalasAGuardar.isEmpty()) {
                escalaRepository.saveAll(escalasAGuardar);
            }

            Set<String> codigosGrupo = new HashSet<>();
            int ordenGrupo = 1;
            List<EstandarItem22JpaEntity> itemsAGuardar = new ArrayList<>();
            for (GuardarEstandarRequest.Grupo22Request gReq : req.grupos22()) {
                if (gReq.titulo() == null || gReq.titulo().isBlank()) {
                    continue;
                }
                String baseCod = gReq.codigo() != null && !gReq.codigo().isBlank()
                        ? normalizarCodigo(gReq.codigo())
                        : normalizarCodigo(gReq.titulo());
                String cod = hacerCodigoUnico(baseCod, codigosGrupo);

                EstandarGrupo22JpaEntity grupo = new EstandarGrupo22JpaEntity();
                grupo.setId(UUID.randomUUID());
                grupo.setEstandar(estandar);
                grupo.setOrden(gReq.orden() != null ? gReq.orden() : ordenGrupo);
                grupo.setCodigo(cod);
                grupo.setTitulo(gReq.titulo().trim());
                grupo.setDescripcion(gReq.descripcion() != null ? gReq.descripcion().trim() : null);
                grupo.setHabilitado(gReq.habilitado() == null || Boolean.TRUE.equals(gReq.habilitado()));
                EstandarGrupo22JpaEntity grupoGuardado = grupoRepository.save(grupo);

                int ordenItem = 1;
                if (gReq.items() != null) {
                    for (GuardarEstandarRequest.Item22Request iReq : gReq.items()) {
                        if (iReq.enunciado() == null || iReq.enunciado().isBlank()) {
                            continue;
                        }
                        EstandarItem22JpaEntity item = new EstandarItem22JpaEntity();
                        item.setId(UUID.randomUUID());
                        item.setGrupo(grupoGuardado);
                        item.setOrden(iReq.orden() != null ? iReq.orden() : ordenItem);
                        item.setEnunciado(iReq.enunciado().trim());
                        itemsAGuardar.add(item);
                        ordenItem++;
                    }
                }
                ordenGrupo++;
            }
            if (!itemsAGuardar.isEmpty()) {
                itemRepository.saveAll(itemsAGuardar);
            }
        }

        Map<TipoSeccionCargaHoraria, Set<String>> codigosPorSeccion = new EnumMap<>(TipoSeccionCargaHoraria.class);
        Map<TipoSeccionCargaHoraria, Integer> ordenPorSeccion = new EnumMap<>(TipoSeccionCargaHoraria.class);
        List<EstandarAreaCargaHorariaJpaEntity> areasAGuardar = new ArrayList<>();
        if (req.areasCargaHoraria() != null) {
            for (GuardarEstandarRequest.AreaCargaHorariaRequest aReq : req.areasCargaHoraria()) {
                if (aReq.nombre() == null || aReq.nombre().isBlank()) {
                    continue;
                }
                TipoSeccionCargaHoraria tipoSec = aReq.tipoSeccion() != null
                        ? aReq.tipoSeccion()
                        : TipoSeccionCargaHoraria.CARGA_TOTAL_311;
                Set<String> codigosUsados = codigosPorSeccion.computeIfAbsent(tipoSec, k -> new HashSet<>());
                int nextOrden = ordenPorSeccion.getOrDefault(tipoSec, 1);
                String baseCod = aReq.codigo() != null && !aReq.codigo().isBlank()
                        ? normalizarCodigo(aReq.codigo())
                        : normalizarCodigo(aReq.nombre());
                String cod = hacerCodigoUnico(baseCod, codigosUsados);

                boolean filaHabilitada = aReq.habilitada() == null || Boolean.TRUE.equals(aReq.habilitada());
                EstandarAreaCargaHorariaJpaEntity area = new EstandarAreaCargaHorariaJpaEntity();
                area.setId(UUID.randomUUID());
                area.setEstandar(estandar);
                area.setTipoSeccion(tipoSec);
                area.setOrden(aReq.orden() != null ? aReq.orden() : nextOrden);
                area.setCodigo(cod);
                area.setNombre(aReq.nombre().trim());
                area.setEtiquetaPractica(aReq.etiquetaPractica() != null && !aReq.etiquetaPractica().isBlank()
                        ? aReq.etiquetaPractica().trim()
                        : aReq.nombre().trim());
                area.setHabilitada(filaHabilitada);
                area.setHorasMinimasCarrera(filaHabilitada ? normalizarHorasMinimas(aReq.horasMinimasCarrera()) : null);
                areasAGuardar.add(area);
                ordenPorSeccion.put(tipoSec, nextOrden + 1);
            }
        }
        if (!areasAGuardar.isEmpty()) {
            areaRepository.saveAll(areasAGuardar);
        }
    }

    private void validarRequest(GuardarEstandarRequest req, boolean requiereCarrera) {
        if (req == null) {
            throw new DomainValidationException("Los datos del estándar son obligatorios.");
        }
        if (requiereCarrera && req.carreraId() == null) {
            throw new DomainValidationException("Debe seleccionar una carrera para el estándar.");
        }
        if (req.nombre() == null || req.nombre().isBlank()) {
            throw new DomainValidationException("El nombre del estándar es obligatorio.");
        }

        TipoSeccionEstandar22 tipo22 = resolverTipoSeccion22(req);
        TipoEvaluacionEstandar22 eval22 = resolverTipoEvaluacion22(req, tipo22);
        if (tipo22 != TipoSeccionEstandar22.NINGUNA && eval22 == TipoEvaluacionEstandar22.NINGUNA) {
            throw new DomainValidationException("Debe seleccionar un método de evaluación válido para la Sección 2.2.");
        }

        if (tipo22 != TipoSeccionEstandar22.NINGUNA) {
            long escalasValidas = req.escalas() == null ? 0 : req.escalas().stream()
                    .filter(e -> e.etiqueta() != null && !e.etiqueta().isBlank())
                    .count();
            if (escalasValidas == 0) {
                throw new DomainValidationException("La Sección 2.2 requiere al menos una opción/nivel de evaluación.");
            }

            List<GuardarEstandarRequest.Grupo22Request> gruposHabilitados = req.grupos22() == null
                    ? List.of()
                    : req.grupos22().stream()
                            .filter(g -> g.titulo() != null && !g.titulo().isBlank())
                            .filter(g -> g.habilitado() == null || Boolean.TRUE.equals(g.habilitado()))
                            .toList();
            if (gruposHabilitados.isEmpty()) {
                throw new DomainValidationException("La Sección 2.2 requiere al menos un bloque o eje habilitado.");
            }

            for (GuardarEstandarRequest.Grupo22Request g : gruposHabilitados) {
                long itemsValidos = g.items() == null ? 0 : g.items().stream()
                        .filter(i -> i.enunciado() != null && !i.enunciado().isBlank())
                        .count();
                if (itemsValidos == 0) {
                    throw new DomainValidationException(
                            "El bloque/eje habilitado \"" + g.titulo().trim() + "\" debe tener al menos un ítem o pregunta.");
                }
            }
        }

        boolean hab311 = req.habilitar311() == null || Boolean.TRUE.equals(req.habilitar311());
        boolean hab312 = req.habilitar312() == null || Boolean.TRUE.equals(req.habilitar312());

        if (req.horasMinimasTotal311() != null && req.horasMinimasTotal311() < 0) {
            throw new DomainValidationException("Las horas mínimas globales del punto 3.1.1 no pueden ser negativas.");
        }
        if (req.horasMinimasTotal312() != null && req.horasMinimasTotal312() < 0) {
            throw new DomainValidationException("Las horas mínimas globales del punto 3.1.2 no pueden ser negativas.");
        }
        if (req.areasCargaHoraria() != null) {
            for (GuardarEstandarRequest.AreaCargaHorariaRequest a : req.areasCargaHoraria()) {
                if (a != null && a.horasMinimasCarrera() != null && a.horasMinimasCarrera() < 0) {
                    throw new DomainValidationException(
                            "Las horas mínimas de carrera no pueden ser negativas (" + (a.nombre() != null ? a.nombre() : "fila") + ").");
                }
            }
        }

        if (hab311) {
            long areasHabilitadas311 = req.areasCargaHoraria() == null ? 0 : req.areasCargaHoraria().stream()
                    .filter(a -> a.nombre() != null && !a.nombre().isBlank())
                    .filter(a -> a.habilitada() == null || Boolean.TRUE.equals(a.habilitada()))
                    .filter(a -> a.tipoSeccion() == null || a.tipoSeccion() == TipoSeccionCargaHoraria.CARGA_TOTAL_311)
                    .count();
            if (areasHabilitadas311 == 0) {
                throw new DomainValidationException(
                        "La Sección 3.1.1 está habilitada y requiere al menos un bloque o área temática activa.");
            }
        }
        if (hab312) {
            long areasHabilitadas312 = req.areasCargaHoraria() == null ? 0 : req.areasCargaHoraria().stream()
                    .filter(a -> a.nombre() != null && !a.nombre().isBlank())
                    .filter(a -> a.habilitada() == null || Boolean.TRUE.equals(a.habilitada()))
                    .filter(a -> a.tipoSeccion() == TipoSeccionCargaHoraria.PRACTICA_312)
                    .count();
            if (areasHabilitadas312 == 0 && req.areasCargaHoraria() != null && req.areasCargaHoraria().stream()
                    .anyMatch(a -> a.tipoSeccion() == TipoSeccionCargaHoraria.PRACTICA_312)) {
                throw new DomainValidationException(
                        "La Sección 3.1.2 está habilitada y requiere al menos una modalidad o área práctica activa.");
            }
        }
    }

    private Integer normalizarHorasMinimas(Integer val) {
        if (val == null || val <= 0) {
            return null;
        }
        return val;
    }

    private TipoSeccionEstandar22 resolverTipoSeccion22(GuardarEstandarRequest req) {
        return req.tipoSeccion22() != null ? req.tipoSeccion22() : TipoSeccionEstandar22.COMPETENCIAS;
    }

    private TipoEvaluacionEstandar22 resolverTipoEvaluacion22(GuardarEstandarRequest req, TipoSeccionEstandar22 tipo22) {
        if (tipo22 == TipoSeccionEstandar22.NINGUNA) {
            return TipoEvaluacionEstandar22.NINGUNA;
        }
        if (req.tipoEvaluacion22() != null) {
            return req.tipoEvaluacion22();
        }
        return tipo22 == TipoSeccionEstandar22.EJES
                ? TipoEvaluacionEstandar22.ESCALA_UNICA
                : TipoEvaluacionEstandar22.MATRIZ_MULTIPLE;
    }

    private void validarPermisoGestionCarrera(CarreraJpaEntity carrera) {
        if (authenticatedUserProvider == null || carrera == null) {
            return;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();
        boolean esAea = tieneRol(user, "AEA");
        if (esAea) {
            return;
        }
        boolean esDirectorEscuela = tieneRol(user, "DIRECTOR_ESCUELA");
        if (!esDirectorEscuela) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo usuarios AEA o Directores de Escuela pueden gestionar estándares.");
        }
        UUID userUaId = user.getUnidadAcademicaId();
        UUID carreraUaId = carrera.getUnidadAcademica() != null ? carrera.getUnidadAcademica().getId() : null;
        if (userUaId == null || !userUaId.equals(carreraUaId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso denegado: el estándar pertenece a una carrera fuera de su Unidad Académica.");
        }
    }

    private UUID resolverUaSiSoloDirectorEscuela() {
        if (authenticatedUserProvider == null) {
            return null;
        }
        Optional<User> userOpt = authenticatedUserProvider.getAuthenticatedUser();
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        if (tieneRol(user, "AEA")) {
            return null;
        }
        if (tieneRol(user, "DIRECTOR_ESCUELA")) {
            return user.getUnidadAcademicaId();
        }
        return null;
    }

    private boolean tieneRol(User user, String code) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r != null && code.equalsIgnoreCase(r.getCode()));
    }

    private String siguienteNombreVersion(String nombreActual) {
        String base = nombreActual != null ? nombreActual.trim() : "Estándar";
        Matcher m = VERSION_SUFFIX_PATTERN.matcher(base);
        if (m.matches()) {
            int nextVer = Integer.parseInt(m.group(2)) + 1;
            String candidate = m.group(1).trim() + " (v" + nextVer + ")";
            return candidate.length() > 180 ? candidate.substring(0, 180) : candidate;
        }
        String candidate = base + " (v2)";
        return candidate.length() > 180 ? candidate.substring(0, 180) : candidate;
    }

    private String normalizarCodigo(String raw) {
        String norm = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (norm.isEmpty()) {
            return "ITEM";
        }
        return norm.length() > 50 ? norm.substring(0, 50) : norm;
    }

    private String hacerCodigoUnico(String base, Set<String> usados) {
        String candidate = base;
        int idx = 2;
        while (usados.contains(candidate)) {
            String suffix = "_" + idx;
            candidate = (base.length() + suffix.length() > 60
                    ? base.substring(0, 60 - suffix.length())
                    : base) + suffix;
            idx++;
        }
        usados.add(candidate);
        return candidate;
    }

    private <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : List.of();
    }
}
