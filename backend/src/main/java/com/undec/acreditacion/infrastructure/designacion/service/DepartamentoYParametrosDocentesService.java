package com.undec.acreditacion.infrastructure.designacion.service;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.infrastructure.designacion.dto.*;
import com.undec.acreditacion.infrastructure.designacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.InstitucionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataInstitucionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class DepartamentoYParametrosDocentesService {

    private final SpringDataDepartamentoAcademicoRepository departamentoRepository;
    private final SpringDataCargoDocenteRepository cargoRepository;
    private final SpringDataDedicacionDocenteRepository dedicacionRepository;
    private final SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository;
    private final SpringDataPeriodoAcademicoRepository periodoRepository;
    private final SpringDataInstitucionRepository institucionRepository;

    public DepartamentoYParametrosDocentesService(
            SpringDataDepartamentoAcademicoRepository departamentoRepository,
            SpringDataCargoDocenteRepository cargoRepository,
            SpringDataDedicacionDocenteRepository dedicacionRepository,
            SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository,
            SpringDataPeriodoAcademicoRepository periodoRepository,
            SpringDataInstitucionRepository institucionRepository
    ) {
        this.departamentoRepository = Objects.requireNonNull(departamentoRepository);
        this.cargoRepository = Objects.requireNonNull(cargoRepository);
        this.dedicacionRepository = Objects.requireNonNull(dedicacionRepository);
        this.tipoDesignacionRepository = Objects.requireNonNull(tipoDesignacionRepository);
        this.periodoRepository = Objects.requireNonNull(periodoRepository);
        this.institucionRepository = institucionRepository;
    }

    @Transactional(readOnly = true)
    public CatalogosDocentesDto obtenerCatalogos(boolean incluirInactivos) {
        return new CatalogosDocentesDto(
                listarDepartamentos(incluirInactivos),
                listarCargos(incluirInactivos),
                listarDedicaciones(incluirInactivos),
                listarTiposDesignacion(incluirInactivos),
                listarPeriodos(incluirInactivos)
        );
    }

    // -------------------------------------------------------------------------
    // Departamentos Académicos (3.2, RF02.1, RN01)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DepartamentoAcademicoDto> listarDepartamentos(boolean incluirInactivos) {
        List<DepartamentoAcademicoJpaEntity> list = incluirInactivos
                ? departamentoRepository.findAllByOrderByDenominacionAsc()
                : departamentoRepository.findByActivoTrueOrderByDenominacionAsc();
        return list.stream().map(this::toDepartamentoDto).toList();
    }

    public DepartamentoAcademicoDto crearDepartamento(GuardarDepartamentoAcademicoRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos del departamento académico son obligatorios.");
        }
        String codigo = normalizarCodigo(req.codigo(), "El código del departamento es obligatorio.");
        String denominacion = requireTexto(req.denominacion(), "La denominación del departamento es obligatoria.");

        if (departamentoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe un Departamento Académico con el código: " + codigo);
        }

        UUID institucionId = institucionRepository != null
                ? institucionRepository.findAll().stream().map(InstitucionJpaEntity::getId).findFirst().orElse(null)
                : null;

        Instant now = Instant.now();
        DepartamentoAcademicoJpaEntity entity = new DepartamentoAcademicoJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setInstitucionId(institucionId);
        entity.setCodigo(codigo);
        entity.setDenominacion(denominacion);
        entity.setDescripcion(trimToNull(req.descripcion()));
        entity.setActivo(req.activo() == null || req.activo());
        entity.setEsSistema(false);
        entity.setCreadoEn(now);
        entity.setActualizadoEn(now);

        return toDepartamentoDto(departamentoRepository.save(entity));
    }

    public DepartamentoAcademicoDto actualizarDepartamento(UUID id, GuardarDepartamentoAcademicoRequest req) {
        if (id == null || req == null) {
            throw new DomainValidationException("El ID y los datos del departamento académico son obligatorios.");
        }
        DepartamentoAcademicoJpaEntity entity = departamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el Departamento Académico ID: " + id));

        String codigo = normalizarCodigo(req.codigo(), "El código del departamento es obligatorio.");
        String denominacion = requireTexto(req.denominacion(), "La denominación del departamento es obligatoria.");

        if (entity.isEsSistema()) {
            if (!entity.getCodigo().equalsIgnoreCase(codigo)) {
                throw new DomainValidationException("No se puede modificar el código del departamento de sistema " + entity.getCodigo() + ".");
            }
            if (req.activo() != null && !req.activo()) {
                throw new DomainValidationException("No se puede desactivar el departamento de sistema " + entity.getCodigo() + ".");
            }
        } else if (!entity.getCodigo().equalsIgnoreCase(codigo) && departamentoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe otro Departamento Académico con el código: " + codigo);
        }

        entity.setCodigo(codigo);
        entity.setDenominacion(denominacion);
        entity.setDescripcion(trimToNull(req.descripcion()));
        if (req.activo() != null) {
            entity.setActivo(req.activo());
        }
        entity.setActualizadoEn(Instant.now());

        return toDepartamentoDto(departamentoRepository.save(entity));
    }

    // -------------------------------------------------------------------------
    // Cargos Docentes (3.3, RF03.1, RN03, RNF06)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CargoDocenteDto> listarCargos(boolean incluirInactivos) {
        List<CargoDocenteJpaEntity> list = incluirInactivos
                ? cargoRepository.findAllByOrderByOrdenAscNombreAsc()
                : cargoRepository.findByActivoTrueOrderByOrdenAscNombreAsc();
        return list.stream().map(this::toCargoDto).toList();
    }

    public CargoDocenteDto crearCargo(GuardarCargoDocenteRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos del cargo docente son obligatorios.");
        }
        String codigo = normalizarCodigo(req.codigo(), "El código del cargo docente es obligatorio.");
        String nombre = requireTexto(req.nombre(), "El nombre del cargo docente es obligatorio.");
        if (cargoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe un cargo docente con el código: " + codigo);
        }

        Instant now = Instant.now();
        CargoDocenteJpaEntity entity = new CargoDocenteJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        entity.setOrden(req.orden() != null && req.orden() > 0 ? req.orden() : 1);
        entity.setActivo(req.activo() == null || req.activo());
        entity.setCreadoEn(now);
        entity.setActualizadoEn(now);

        return toCargoDto(cargoRepository.save(entity));
    }

    public CargoDocenteDto actualizarCargo(UUID id, GuardarCargoDocenteRequest req) {
        if (id == null || req == null) {
            throw new DomainValidationException("El ID y los datos del cargo docente son obligatorios.");
        }
        CargoDocenteJpaEntity entity = cargoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cargo docente ID: " + id));

        String codigo = normalizarCodigo(req.codigo(), "El código del cargo docente es obligatorio.");
        String nombre = requireTexto(req.nombre(), "El nombre del cargo docente es obligatorio.");
        if (!entity.getCodigo().equalsIgnoreCase(codigo) && cargoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe otro cargo docente con el código: " + codigo);
        }

        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        if (req.orden() != null && req.orden() > 0) {
            entity.setOrden(req.orden());
        }
        if (req.activo() != null) {
            entity.setActivo(req.activo());
        }
        entity.setActualizadoEn(Instant.now());

        return toCargoDto(cargoRepository.save(entity));
    }

    // -------------------------------------------------------------------------
    // Dedicaciones Docentes (3.4, RF03.2, RN04, RNF06)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DedicacionDocenteDto> listarDedicaciones(boolean incluirInactivos) {
        List<DedicacionDocenteJpaEntity> list = incluirInactivos
                ? dedicacionRepository.findAllByOrderByOrdenAscHorasSemanalesDesc()
                : dedicacionRepository.findByActivoTrueOrderByOrdenAscHorasSemanalesDesc();
        return list.stream().map(this::toDedicacionDto).toList();
    }

    public DedicacionDocenteDto crearDedicacion(GuardarDedicacionDocenteRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos de la dedicación docente son obligatorios.");
        }
        String codigo = normalizarCodigo(req.codigo(), "El código de la dedicación es obligatorio.");
        String nombre = requireTexto(req.nombre(), "El nombre de la dedicación es obligatorio.");
        validarHorasDedicacion(req.horasSemanales());
        if (dedicacionRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe una dedicación docente con el código: " + codigo);
        }

        Instant now = Instant.now();
        DedicacionDocenteJpaEntity entity = new DedicacionDocenteJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        entity.setHorasSemanales(req.horasSemanales());
        entity.setOrden(req.orden() != null && req.orden() > 0 ? req.orden() : 1);
        entity.setActivo(req.activo() == null || req.activo());
        entity.setCreadoEn(now);
        entity.setActualizadoEn(now);

        return toDedicacionDto(dedicacionRepository.save(entity));
    }

    public DedicacionDocenteDto actualizarDedicacion(UUID id, GuardarDedicacionDocenteRequest req) {
        if (id == null || req == null) {
            throw new DomainValidationException("El ID y los datos de la dedicación docente son obligatorios.");
        }
        DedicacionDocenteJpaEntity entity = dedicacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la dedicación docente ID: " + id));

        String codigo = normalizarCodigo(req.codigo(), "El código de la dedicación es obligatorio.");
        String nombre = requireTexto(req.nombre(), "El nombre de la dedicación es obligatorio.");
        validarHorasDedicacion(req.horasSemanales());
        if (!entity.getCodigo().equalsIgnoreCase(codigo) && dedicacionRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe otra dedicación docente con el código: " + codigo);
        }

        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        entity.setHorasSemanales(req.horasSemanales());
        if (req.orden() != null && req.orden() > 0) {
            entity.setOrden(req.orden());
        }
        if (req.activo() != null) {
            entity.setActivo(req.activo());
        }
        entity.setActualizadoEn(Instant.now());

        return toDedicacionDto(dedicacionRepository.save(entity));
    }

    // -------------------------------------------------------------------------
    // Tipos de Designación Docente (RF10.3, RNF06)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TipoDesignacionDocenteDto> listarTiposDesignacion(boolean incluirInactivos) {
        List<TipoDesignacionDocenteJpaEntity> list = incluirInactivos
                ? tipoDesignacionRepository.findAllByOrderByOrdenAscNombreAsc()
                : tipoDesignacionRepository.findByActivoTrueOrderByOrdenAscNombreAsc();
        return list.stream().map(this::toTipoDesignacionDto).toList();
    }

    public TipoDesignacionDocenteDto crearTipoDesignacion(GuardarTipoDesignacionRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos del tipo de designación son obligatorios.");
        }
        String codigo = normalizarCodigo(req.codigo(), "El código del tipo de designación es obligatorio.");
        String nombre = requireTexto(req.nombre(), "El nombre del tipo de designación es obligatorio.");
        if (tipoDesignacionRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe un tipo de designación con el código: " + codigo);
        }

        Instant now = Instant.now();
        TipoDesignacionDocenteJpaEntity entity = new TipoDesignacionDocenteJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        entity.setEsRentado(req.esRentado() == null || req.esRentado());
        entity.setOrden(req.orden() != null && req.orden() > 0 ? req.orden() : 1);
        entity.setActivo(req.activo() == null || req.activo());
        entity.setCreadoEn(now);
        entity.setActualizadoEn(now);

        return toTipoDesignacionDto(tipoDesignacionRepository.save(entity));
    }

    public TipoDesignacionDocenteDto actualizarTipoDesignacion(UUID id, GuardarTipoDesignacionRequest req) {
        if (id == null || req == null) {
            throw new DomainValidationException("El ID y los datos del tipo de designación son obligatorios.");
        }
        TipoDesignacionDocenteJpaEntity entity = tipoDesignacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tipo de designación ID: " + id));

        String codigo = normalizarCodigo(req.codigo(), "El código del tipo de designación es obligatorio.");
        String nombre = requireTexto(req.nombre(), "El nombre del tipo de designación es obligatorio.");
        if (!entity.getCodigo().equalsIgnoreCase(codigo) && tipoDesignacionRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe otro tipo de designación con el código: " + codigo);
        }

        entity.setCodigo(codigo);
        entity.setNombre(nombre);
        if (req.esRentado() != null) {
            entity.setEsRentado(req.esRentado());
        }
        if (req.orden() != null && req.orden() > 0) {
            entity.setOrden(req.orden());
        }
        if (req.activo() != null) {
            entity.setActivo(req.activo());
        }
        entity.setActualizadoEn(Instant.now());

        return toTipoDesignacionDto(tipoDesignacionRepository.save(entity));
    }

    // -------------------------------------------------------------------------
    // Períodos Académicos
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PeriodoAcademicoDto> listarPeriodos(boolean incluirInactivos) {
        List<PeriodoAcademicoJpaEntity> list = incluirInactivos
                ? periodoRepository.findAllByOrderByAnioDescCodigoDesc()
                : periodoRepository.findByActivoTrueOrderByAnioDescCodigoDesc();
        return list.stream().map(this::toPeriodoDto).toList();
    }

    public PeriodoAcademicoDto crearPeriodo(GuardarPeriodoAcademicoRequest req) {
        if (req == null) {
            throw new DomainValidationException("Los datos del período académico son obligatorios.");
        }
        String codigo = normalizarCodigo(req.codigo(), "El código del período académico es obligatorio.");
        String denominacion = requireTexto(req.denominacion(), "La denominación del período académico es obligatoria.");
        validarPeriodoFechasYAnio(req);
        if (periodoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe un período académico con el código: " + codigo);
        }

        boolean esActual = Boolean.TRUE.equals(req.esActual());
        if (esActual) {
            desmarcarOtrosPeriodosActuales(null);
        }

        Instant now = Instant.now();
        PeriodoAcademicoJpaEntity entity = new PeriodoAcademicoJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setCodigo(codigo);
        entity.setDenominacion(denominacion);
        entity.setAnio(req.anio());
        entity.setFechaInicio(req.fechaInicio());
        entity.setFechaFin(req.fechaFin());
        entity.setEsActual(esActual);
        entity.setActivo(req.activo() == null || req.activo());
        entity.setCreadoEn(now);
        entity.setActualizadoEn(now);

        return toPeriodoDto(periodoRepository.save(entity));
    }

    public PeriodoAcademicoDto actualizarPeriodo(UUID id, GuardarPeriodoAcademicoRequest req) {
        if (id == null || req == null) {
            throw new DomainValidationException("El ID y los datos del período académico son obligatorios.");
        }
        PeriodoAcademicoJpaEntity entity = periodoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el período académico ID: " + id));

        String codigo = normalizarCodigo(req.codigo(), "El código del período académico es obligatorio.");
        String denominacion = requireTexto(req.denominacion(), "La denominación del período académico es obligatoria.");
        validarPeriodoFechasYAnio(req);
        if (!entity.getCodigo().equalsIgnoreCase(codigo) && periodoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new DomainValidationException("Ya existe otro período académico con el código: " + codigo);
        }

        if (Boolean.TRUE.equals(req.esActual())) {
            desmarcarOtrosPeriodosActuales(entity.getId());
        }

        entity.setCodigo(codigo);
        entity.setDenominacion(denominacion);
        entity.setAnio(req.anio());
        entity.setFechaInicio(req.fechaInicio());
        entity.setFechaFin(req.fechaFin());
        if (req.esActual() != null) {
            entity.setEsActual(req.esActual());
        }
        if (req.activo() != null) {
            entity.setActivo(req.activo());
        }
        entity.setActualizadoEn(Instant.now());

        return toPeriodoDto(periodoRepository.save(entity));
    }

    private void desmarcarOtrosPeriodosActuales(UUID exceptId) {
        for (PeriodoAcademicoJpaEntity otro : periodoRepository.findAll()) {
            if (otro.isEsActual() && (exceptId == null || !exceptId.equals(otro.getId()))) {
                otro.setEsActual(false);
                otro.setActualizadoEn(Instant.now());
                periodoRepository.save(otro);
            }
        }
    }

    private static void validarHorasDedicacion(Integer horas) {
        if (horas == null || horas <= 0 || horas > 50) {
            throw new DomainValidationException("Las horas semanales de la dedicación deben estar entre 1 y 50 hs.");
        }
    }

    private static void validarPeriodoFechasYAnio(GuardarPeriodoAcademicoRequest req) {
        if (req.anio() == null || req.anio() < 1990 || req.anio() > 2100) {
            throw new DomainValidationException("El año del período académico debe estar entre 1990 y 2100.");
        }
        if (req.fechaInicio() != null && req.fechaFin() != null && req.fechaFin().isBefore(req.fechaInicio())) {
            throw new DomainValidationException("La fecha de fin del período no puede ser anterior a la fecha de inicio.");
        }
    }

    private static String normalizarCodigo(String raw, String msg) {
        if (raw == null || raw.isBlank()) {
            throw new DomainValidationException(msg);
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String requireTexto(String raw, String msg) {
        if (raw == null || raw.isBlank()) {
            throw new DomainValidationException(msg);
        }
        return raw.trim();
    }

    private static String trimToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }

    private DepartamentoAcademicoDto toDepartamentoDto(DepartamentoAcademicoJpaEntity e) {
        return new DepartamentoAcademicoDto(
                e.getId(),
                e.getInstitucionId(),
                e.getCodigo(),
                e.getDenominacion(),
                e.getDescripcion(),
                e.isActivo(),
                e.isEsSistema(),
                e.getCreadoEn(),
                e.getActualizadoEn()
        );
    }

    private CargoDocenteDto toCargoDto(CargoDocenteJpaEntity e) {
        return new CargoDocenteDto(e.getId(), e.getCodigo(), e.getNombre(), e.getOrden(), e.isActivo());
    }

    private DedicacionDocenteDto toDedicacionDto(DedicacionDocenteJpaEntity e) {
        return new DedicacionDocenteDto(e.getId(), e.getCodigo(), e.getNombre(), e.getHorasSemanales(), e.getOrden(), e.isActivo());
    }

    private TipoDesignacionDocenteDto toTipoDesignacionDto(TipoDesignacionDocenteJpaEntity e) {
        return new TipoDesignacionDocenteDto(e.getId(), e.getCodigo(), e.getNombre(), e.isEsRentado(), e.getOrden(), e.isActivo());
    }

    private PeriodoAcademicoDto toPeriodoDto(PeriodoAcademicoJpaEntity e) {
        return new PeriodoAcademicoDto(
                e.getId(), e.getCodigo(), e.getDenominacion(), e.getAnio(),
                e.getFechaInicio(), e.getFechaFin(), e.isEsActual(), e.isActivo()
        );
    }
}
