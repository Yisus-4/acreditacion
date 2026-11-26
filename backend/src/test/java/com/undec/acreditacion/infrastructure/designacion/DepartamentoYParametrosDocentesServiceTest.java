package com.undec.acreditacion.infrastructure.designacion;

import com.undec.acreditacion.domain.entities.CodigosRol;
import com.undec.acreditacion.domain.entities.EstadoDictadoActividad;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.ActividadCurricularPlanJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.PlanEstudioFt001JpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataActividadCurricularPlanRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataAmbitoPracticaRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataEspacioFisicoRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.CrearCuerpoAcademicoMaestroRequest;
import com.undec.acreditacion.infrastructure.datoscentrales.dto.CuerpoAcademicoMaestroDto;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.SpringDataCuerpoAcademicoMaestroRepository;
import com.undec.acreditacion.infrastructure.datoscentrales.service.DatosCentralesService;
import com.undec.acreditacion.infrastructure.designacion.dto.*;
import com.undec.acreditacion.infrastructure.designacion.persistence.*;
import com.undec.acreditacion.infrastructure.designacion.service.DepartamentoYParametrosDocentesService;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataInstitucionRepository;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartamentoYParametrosDocentesServiceTest {

    @Mock
    private SpringDataDepartamentoAcademicoRepository departamentoRepository;
    @Mock
    private SpringDataCargoDocenteRepository cargoRepository;
    @Mock
    private SpringDataDedicacionDocenteRepository dedicacionRepository;
    @Mock
    private SpringDataTipoDesignacionDocenteRepository tipoDesignacionRepository;
    @Mock
    private SpringDataPeriodoAcademicoRepository periodoRepository;
    @Mock
    private SpringDataInstitucionRepository institucionRepository;

    @Mock
    private SpringDataPlanEstudioFt001Repository planRepository;
    @Mock
    private SpringDataCuerpoAcademicoMaestroRepository docenteRepository;
    @Mock
    private SpringDataEspacioFisicoRepository espacioRepository;
    @Mock
    private SpringDataAmbitoPracticaRepository ambitoRepository;
    @Mock
    private SpringDataCarreraRepository carreraRepository;
    @Mock
    private SpringDataActividadCurricularPlanRepository actividadRepository;
    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    private DepartamentoYParametrosDocentesService parametrosService;
    private DatosCentralesService datosCentralesService;

    @BeforeEach
    void setUp() {
        parametrosService = new DepartamentoYParametrosDocentesService(
                departamentoRepository,
                cargoRepository,
                dedicacionRepository,
                tipoDesignacionRepository,
                periodoRepository,
                institucionRepository
        );
        datosCentralesService = new DatosCentralesService(
                planRepository,
                docenteRepository,
                espacioRepository,
                ambitoRepository,
                carreraRepository,
                actividadRepository,
                null,
                null,
                departamentoRepository,
                authenticatedUserProvider
        );
    }

    @Test
    @DisplayName("RF02.1: Crea departamento normalizando código a mayúsculas y rechaza duplicados case-insensitive")
    void creaDepartamentoYValidaCodigoUnico() {
        when(institucionRepository.findAll()).thenReturn(List.of());
        when(departamentoRepository.existsByCodigoIgnoreCase("CBASICAS")).thenReturn(false);
        when(departamentoRepository.save(any(DepartamentoAcademicoJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DepartamentoAcademicoDto creado = parametrosService.crearDepartamento(
                new GuardarDepartamentoAcademicoRequest(" cbasicas ", "Ciencias Básicas y Tecnológicas", "Desc", true)
        );

        assertEquals("CBASICAS", creado.codigo());
        assertEquals("Ciencias Básicas y Tecnológicas", creado.denominacion());
        assertTrue(creado.activo());

        when(departamentoRepository.existsByCodigoIgnoreCase("CBASICAS")).thenReturn(true);
        assertThrows(DomainValidationException.class, () -> parametrosService.crearDepartamento(
                new GuardarDepartamentoAcademicoRequest("cbasicas", "Otro nombre", null, true)
        ));
    }

    @Test
    @DisplayName("No permite desactivar ni cambiar el código del departamento de sistema SIN_ASIGNAR")
    void protegeDepartamentoDeSistemaSinAsignar() {
        UUID id = UUID.randomUUID();
        DepartamentoAcademicoJpaEntity sinAsignar = new DepartamentoAcademicoJpaEntity();
        sinAsignar.setId(id);
        sinAsignar.setCodigo("SIN_ASIGNAR");
        sinAsignar.setDenominacion("Sin asignar / A clasificar");
        sinAsignar.setActivo(true);
        sinAsignar.setEsSistema(true);

        when(departamentoRepository.findById(id)).thenReturn(Optional.of(sinAsignar));

        assertThrows(DomainValidationException.class, () -> parametrosService.actualizarDepartamento(
                id, new GuardarDepartamentoAcademicoRequest("OTRO_COD", "Sin asignar", null, true)
        ));
        assertThrows(DomainValidationException.class, () -> parametrosService.actualizarDepartamento(
                id, new GuardarDepartamentoAcademicoRequest("SIN_ASIGNAR", "Sin asignar", null, false)
        ));
    }

    @Test
    @DisplayName("RN04 / RF03.2: Valida horas semanales válidas (1..50) al parametrizar dedicaciones docentes")
    void validaHorasEnDedicacionesDocentes() {
        assertThrows(DomainValidationException.class, () -> parametrosService.crearDedicacion(
                new GuardarDedicacionDocenteRequest("CERO", "Inválida", 0, 1, true)
        ));
        assertThrows(DomainValidationException.class, () -> parametrosService.crearDedicacion(
                new GuardarDedicacionDocenteRequest("EXCESO", "Inválida", 60, 1, true)
        ));

        when(dedicacionRepository.existsByCodigoIgnoreCase("PARCIAL")).thenReturn(false);
        when(dedicacionRepository.save(any(DedicacionDocenteJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        DedicacionDocenteDto dto = parametrosService.crearDedicacion(
                new GuardarDedicacionDocenteRequest("parcial", "Parcial", 20, 2, true)
        );
        assertEquals("PARCIAL", dto.codigo());
        assertEquals(20, dto.horasSemanales());
    }

    @Test
    @DisplayName("Período académico actual desmarca el período actual previo")
    void marcarPeriodoActualDesmarcaAnteriores() {
        PeriodoAcademicoJpaEntity p2025 = new PeriodoAcademicoJpaEntity();
        p2025.setId(UUID.randomUUID());
        p2025.setCodigo("2025");
        p2025.setAnio(2025);
        p2025.setEsActual(true);

        when(periodoRepository.existsByCodigoIgnoreCase("2026")).thenReturn(false);
        when(periodoRepository.findAll()).thenReturn(List.of(p2025));
        when(periodoRepository.save(any(PeriodoAcademicoJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PeriodoAcademicoDto creado = parametrosService.crearPeriodo(
                new GuardarPeriodoAcademicoRequest("2026", "Ciclo Lectivo 2026", 2026,
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31), true, true)
        );

        assertTrue(creado.esActual());
        assertFalse(p2025.isEsActual());
    }

    @Test
    @DisplayName("RN01 / RF01.6: Registra docente con Departamento, TipoDocumento, Legajo normalizado (vacío -> null) y Mapuche")
    void registraDocenteConDepartamentoLegajoYMapuche() {
        UUID deptoId = UUID.randomUUID();
        DepartamentoAcademicoJpaEntity depto = new DepartamentoAcademicoJpaEntity();
        depto.setId(deptoId);
        depto.setCodigo("CBASICAS");
        depto.setDenominacion("Departamento de Ciencias Básicas");

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(docenteRepository.existsByDni("30111222")).thenReturn(false);
        when(departamentoRepository.findById(deptoId)).thenReturn(Optional.of(depto));
        when(docenteRepository.save(any(CuerpoAcademicoMaestroJpaEntity.class))).thenAnswer(inv -> {
            CuerpoAcademicoMaestroJpaEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // Caso 1: legajo en blanco se guarda como null (no dispara choque de unicidad)
        CuerpoAcademicoMaestroDto sinLegajo = datosCentralesService.crearCuerpoAcademicoMaestro(
                new CrearCuerpoAcademicoMaestroRequest(
                        "PASAPORTE", "30111222", "20-30111222-3", "   ", "MAP-9001", deptoId,
                        "Gómez", "Ana", "agomez@undec.edu.ar", null, null,
                        "Licenciada", "Doctora", "TITULAR", "REGULAR_RENTADO", "EXCLUSIVA", 40, true, List.of()
                )
        );
        assertNull(sinLegajo.legajo());
        assertEquals("PASAPORTE", sinLegajo.tipoDocumento());
        assertEquals("MAP-9001", sinLegajo.nroDesignacionMapuche());
        assertEquals(deptoId, sinLegajo.departamentoAcademicoId());

        // Caso 2: legajo informado pero duplicado lanza 422
        CuerpoAcademicoMaestroJpaEntity existenteConLegajo = new CuerpoAcademicoMaestroJpaEntity();
        existenteConLegajo.setId(UUID.randomUUID());
        existenteConLegajo.setLegajo("LEG-100");
        when(docenteRepository.existsByDni("31999888")).thenReturn(false);
        when(docenteRepository.findByLegajoIgnoreCase("LEG-100")).thenReturn(Optional.of(existenteConLegajo));

        assertThrows(DomainValidationException.class, () -> datosCentralesService.crearCuerpoAcademicoMaestro(
                new CrearCuerpoAcademicoMaestroRequest(
                        "DNI", "31999888", null, "leg-100", null, deptoId,
                        "Pérez", "Luis", null, null, null,
                        null, null, "JTP", "INTERINO_RENTADO", "SIMPLE", 10, true, List.of()
                )
        ));
    }

    @Test
    @DisplayName("RNF01: DIRECTOR_DEPARTAMENTO solo puede registrar/editar docentes de su propio Departamento Académico")
    void directorDepartamentoAisladoASuPropioDepartamento() {
        UUID miDeptoId = UUID.randomUUID();
        UUID otroDeptoId = UUID.randomUUID();

        Role rolDirectorDepto = Role.system(UUID.randomUUID(), CodigosRol.DIRECTOR_DEPARTAMENTO, "Director Depto", "Desc");
        User director = User.rehydrate(
                UUID.randomUUID(), "dir_basicas", "dir@undec.edu.ar", "hash",
                true, false, null, miDeptoId, Set.of(rolDirectorDepto)
        );
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(director));

        assertThrows(DomainValidationException.class, () -> datosCentralesService.crearCuerpoAcademicoMaestro(
                new CrearCuerpoAcademicoMaestroRequest(
                        "DNI", "32555666", null, null, null, otroDeptoId,
                        "Díaz", "Mario", null, null, null,
                        null, null, "ADJUNTO", "INTERINO_RENTADO", "PARCIAL", 20, true, List.of()
                )
        ));
    }

    @Test
    @DisplayName("RNF01: DIRECTOR_DEPARTAMENTO mantiene aislamiento de lectura incluso filtrando por carreraId")
    void directorDepartamentoNoPuedeLeerDocentesDeOtroDepartamentoConCarreraId() {
        UUID miDeptoId = UUID.randomUUID();
        UUID otroDeptoId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();

        DepartamentoAcademicoJpaEntity miDepto = new DepartamentoAcademicoJpaEntity();
        miDepto.setId(miDeptoId);
        miDepto.setCodigo("CBASICAS");
        miDepto.setDenominacion("Ciencias Básicas");

        DepartamentoAcademicoJpaEntity otroDepto = new DepartamentoAcademicoJpaEntity();
        otroDepto.setId(otroDeptoId);
        otroDepto.setCodigo("CSOCIALES");
        otroDepto.setDenominacion("Ciencias Sociales");

        CuerpoAcademicoMaestroJpaEntity docMio = new CuerpoAcademicoMaestroJpaEntity();
        docMio.setId(UUID.randomUUID());
        docMio.setDni("25000111");
        docMio.setApellido("Álvarez");
        docMio.setNombre("Sofía");
        docMio.setDepartamentoAcademico(miDepto);
        docMio.setActivo(true);

        CuerpoAcademicoMaestroJpaEntity docAjeno = new CuerpoAcademicoMaestroJpaEntity();
        docAjeno.setId(UUID.randomUUID());
        docAjeno.setDni("26000222");
        docAjeno.setApellido("Benítez");
        docAjeno.setNombre("Carlos");
        docAjeno.setDepartamentoAcademico(otroDepto);
        docAjeno.setActivo(true);

        Role rolDirectorDepto = Role.system(UUID.randomUUID(), CodigosRol.DIRECTOR_DEPARTAMENTO, "Director Depto", "Desc");
        User director = User.rehydrate(
                UUID.randomUUID(), "dir_basicas", "dir@undec.edu.ar", "hash",
                true, false, null, miDeptoId, Set.of(rolDirectorDepto)
        );
        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(director));
        when(docenteRepository.findByCarreraId(carreraId)).thenReturn(List.of(docMio, docAjeno));

        List<CuerpoAcademicoMaestroDto> resultado = datosCentralesService.listarCuerpoAcademico(
                carreraId, otroDeptoId, false, null
        );

        assertEquals(1, resultado.size());
        assertEquals("25000111", resultado.get(0).dni());
    }

    @Test
    @DisplayName("RN09: Rechaza asignar un docente a una actividad curricular en situación INACTIVA")
    void rechazaAsignarDocenteAActividadInactiva() {
        UUID docenteId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID actId = UUID.randomUUID();

        CuerpoAcademicoMaestroJpaEntity docente = new CuerpoAcademicoMaestroJpaEntity();
        docente.setId(docenteId);

        PlanEstudioFt001JpaEntity plan = new PlanEstudioFt001JpaEntity();
        plan.setId(planId);

        ActividadCurricularPlanJpaEntity actInactiva = new ActividadCurricularPlanJpaEntity();
        actInactiva.setId(actId);
        actInactiva.setPlanEstudio(plan);
        actInactiva.setEstadoDictado(EstadoDictadoActividad.INACTIVA);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.empty());
        when(docenteRepository.findById(docenteId)).thenReturn(Optional.of(docente));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(actividadRepository.findById(actId)).thenReturn(Optional.of(actInactiva));

        assertThrows(DomainValidationException.class, () -> datosCentralesService.agregarAsignacionDocente(
                docenteId,
                new CrearCuerpoAcademicoMaestroRequest.CrearAsignacionCatedraItem(
                        UUID.randomUUID(), planId, actId, "PROFESOR_A_CARGO", 4
                )
        ));
    }
}
