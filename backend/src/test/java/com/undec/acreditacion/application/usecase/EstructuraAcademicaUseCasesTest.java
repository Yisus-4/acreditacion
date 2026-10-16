package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ActualizarCarreraCommand;
import com.undec.acreditacion.application.input.ActualizarSedeLocalizacionCommand;
import com.undec.acreditacion.application.input.ActualizarUnidadAcademicaCommand;
import com.undec.acreditacion.application.input.RegistrarCarreraCommand;
import com.undec.acreditacion.application.input.RegistrarSedeLocalizacionCommand;
import com.undec.acreditacion.application.input.RegistrarUnidadAcademicaCommand;
import com.undec.acreditacion.domain.entities.Carrera;
import com.undec.acreditacion.domain.entities.EstadoCarrera;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.entities.ModalidadCarrera;
import com.undec.acreditacion.domain.entities.SedeLocalizacion;
import com.undec.acreditacion.domain.entities.UnidadAcademica;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;
import com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort;
import com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EstructuraAcademicaUseCasesTest {

    private UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    private SedeLocalizacionRepositoryPort sedeLocalizacionRepository;
    private CarreraRepositoryPort carreraRepository;
    private InstitucionRepositoryPort institucionRepository;

    private RegistrarUnidadAcademicaService registrarUnidadAcademicaService;
    private ListarUnidadesAcademicasService listarUnidadesAcademicasService;
    private ActualizarUnidadAcademicaService actualizarUnidadAcademicaService;
    private CambiarEstadoUnidadAcademicaService cambiarEstadoUnidadAcademicaService;
    private EliminarUnidadAcademicaService eliminarUnidadAcademicaService;

    private RegistrarSedeLocalizacionService registrarSedeLocalizacionService;
    private ListarSedesLocalizacionesService listarSedesLocalizacionesService;
    private ActualizarSedeLocalizacionService actualizarSedeLocalizacionService;
    private CambiarEstadoSedeLocalizacionService cambiarEstadoSedeLocalizacionService;
    private EliminarSedeLocalizacionService eliminarSedeLocalizacionService;

    private RegistrarCarreraService registrarCarreraService;
    private ListarCarrerasService listarCarrerasService;
    private ActualizarCarreraService actualizarCarreraService;
    private CambiarEstadoCarreraService cambiarEstadoCarreraService;
    private EliminarCarreraService eliminarCarreraService;

    @BeforeEach
    void setUp() {
        unidadAcademicaRepository = mock(UnidadAcademicaRepositoryPort.class);
        sedeLocalizacionRepository = mock(SedeLocalizacionRepositoryPort.class);
        carreraRepository = mock(CarreraRepositoryPort.class);
        institucionRepository = mock(InstitucionRepositoryPort.class);

        registrarUnidadAcademicaService = new RegistrarUnidadAcademicaService(unidadAcademicaRepository, institucionRepository);
        listarUnidadesAcademicasService = new ListarUnidadesAcademicasService(unidadAcademicaRepository);
        actualizarUnidadAcademicaService = new ActualizarUnidadAcademicaService(unidadAcademicaRepository);
        cambiarEstadoUnidadAcademicaService = new CambiarEstadoUnidadAcademicaService(unidadAcademicaRepository);
        eliminarUnidadAcademicaService = new EliminarUnidadAcademicaService(unidadAcademicaRepository, carreraRepository);

        registrarSedeLocalizacionService = new RegistrarSedeLocalizacionService(sedeLocalizacionRepository, institucionRepository);
        listarSedesLocalizacionesService = new ListarSedesLocalizacionesService(sedeLocalizacionRepository);
        actualizarSedeLocalizacionService = new ActualizarSedeLocalizacionService(sedeLocalizacionRepository);
        cambiarEstadoSedeLocalizacionService = new CambiarEstadoSedeLocalizacionService(sedeLocalizacionRepository);
        eliminarSedeLocalizacionService = new EliminarSedeLocalizacionService(sedeLocalizacionRepository, carreraRepository);

        registrarCarreraService = new RegistrarCarreraService(carreraRepository, unidadAcademicaRepository, sedeLocalizacionRepository);
        listarCarrerasService = new ListarCarrerasService(carreraRepository);
        actualizarCarreraService = new ActualizarCarreraService(carreraRepository, sedeLocalizacionRepository);
        cambiarEstadoCarreraService = new CambiarEstadoCarreraService(carreraRepository);
        eliminarCarreraService = new EliminarCarreraService(carreraRepository);
    }

    // ──────────────────────────── Unidad Académica ────────────────────────────

    @Test
    @DisplayName("Registrar Unidad Académica exitosamente")
    void registrarUnidadAcademicaExitoso() {
        UUID instId = UUID.randomUUID();
        when(institucionRepository.findById(instId)).thenReturn(Optional.of(mock(Institucion.class)));
        when(unidadAcademicaRepository.existsByInstitucionIdAndNombre(instId, "Escuela de Ingeniería")).thenReturn(false);
        when(unidadAcademicaRepository.save(any(UnidadAcademica.class))).thenAnswer(i -> i.getArgument(0));

        RegistrarUnidadAcademicaCommand cmd = new RegistrarUnidadAcademicaCommand(instId, "Escuela de Ingeniería", "EI", "Desc");
        UnidadAcademica ua = registrarUnidadAcademicaService.registrar(cmd);

        assertNotNull(ua);
        assertEquals("Escuela de Ingeniería", ua.getNombre());
        verify(unidadAcademicaRepository).save(any(UnidadAcademica.class));
    }

    @Test
    @DisplayName("Registrar Unidad Académica falla si la institución no existe")
    void registrarUnidadAcademicaInstitucionInexistente() {
        UUID instId = UUID.randomUUID();
        when(institucionRepository.findById(instId)).thenReturn(Optional.empty());

        RegistrarUnidadAcademicaCommand cmd = new RegistrarUnidadAcademicaCommand(instId, "Escuela de Ingeniería", "EI", "Desc");
        assertThrows(DomainValidationException.class, () -> registrarUnidadAcademicaService.registrar(cmd));
    }

    @Test
    @DisplayName("Actualizar Unidad Académica exitosamente")
    void actualizarUnidadAcademicaExitosa() {
        UUID uaId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(instId, "Viejo Nombre", "VN", "Vieja desc");
        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(unidadAcademicaRepository.existsByInstitucionIdAndNombre(instId, "Nuevo Nombre")).thenReturn(false);
        when(unidadAcademicaRepository.save(any(UnidadAcademica.class))).thenAnswer(i -> i.getArgument(0));

        ActualizarUnidadAcademicaCommand cmd = new ActualizarUnidadAcademicaCommand(uaId, "Nuevo Nombre", "NN", "Nueva desc");
        UnidadAcademica actualizada = actualizarUnidadAcademicaService.actualizar(cmd);

        assertEquals("Nuevo Nombre", actualizada.getNombre());
        assertEquals("NN", actualizada.getSigla());
    }

    @Test
    @DisplayName("Actualizar Unidad Académica falla si ya existe otra con el mismo nombre")
    void actualizarUnidadAcademicaFallaNombreDuplicado() {
        UUID uaId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(instId, "Viejo Nombre", "VN", "Vieja desc");
        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(unidadAcademicaRepository.existsByInstitucionIdAndNombre(instId, "Nombre Existente")).thenReturn(true);

        ActualizarUnidadAcademicaCommand cmd = new ActualizarUnidadAcademicaCommand(uaId, "Nombre Existente", "NE", "Desc");
        assertThrows(DomainValidationException.class, () -> actualizarUnidadAcademicaService.actualizar(cmd));
    }

    @Test
    @DisplayName("Cambiar estado Unidad Académica exitosamente")
    void cambiarEstadoUnidadAcademicaExitosa() {
        UUID uaId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(UUID.randomUUID(), "Escuela", "E", "Desc");
        assertTrue(ua.isActivo());
        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(unidadAcademicaRepository.save(any(UnidadAcademica.class))).thenAnswer(i -> i.getArgument(0));

        UnidadAcademica desactivada = cambiarEstadoUnidadAcademicaService.cambiarEstado(uaId, false);
        assertFalse(desactivada.isActivo());
    }

    @Test
    @DisplayName("Eliminar Unidad Académica exitosamente cuando no tiene carreras asociadas")
    void eliminarUnidadAcademicaExitosa() {
        UUID uaId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(UUID.randomUUID(), "Escuela", "E", "Desc");
        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(carreraRepository.countByUnidadAcademicaId(uaId)).thenReturn(0L);

        eliminarUnidadAcademicaService.eliminar(uaId);
        verify(unidadAcademicaRepository).deleteById(uaId);
    }

    @Test
    @DisplayName("Eliminar Unidad Académica falla si tiene carreras asociadas")
    void eliminarUnidadAcademicaFallaPorCarrerasAsociadas() {
        UUID uaId = UUID.randomUUID();
        UnidadAcademica ua = UnidadAcademica.registrar(UUID.randomUUID(), "Escuela", "E", "Desc");
        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.of(ua));
        when(carreraRepository.countByUnidadAcademicaId(uaId)).thenReturn(3L);

        DomainValidationException ex = assertThrows(DomainValidationException.class,
                () -> eliminarUnidadAcademicaService.eliminar(uaId));
        assertTrue(ex.getMessage().contains("tiene carreras asociadas"));
    }

    // ──────────────────────────── Sede Localización ────────────────────────────

    @Test
    @DisplayName("Registrar Sede exitosamente")
    void registrarSedeExitosa() {
        UUID instId = UUID.randomUUID();
        when(institucionRepository.findById(instId)).thenReturn(Optional.of(mock(Institucion.class)));
        when(sedeLocalizacionRepository.save(any(SedeLocalizacion.class))).thenAnswer(i -> i.getArgument(0));

        RegistrarSedeLocalizacionCommand cmd = new RegistrarSedeLocalizacionCommand(
                instId, "Campus Los Sarmientos", "Ruta Los Peregrinos", "s/n", null, "5361", 1L, 2L, 3L, true
        );
        SedeLocalizacion sede = registrarSedeLocalizacionService.registrar(cmd);

        assertNotNull(sede);
        assertEquals("Campus Los Sarmientos", sede.getNombre());
        verify(sedeLocalizacionRepository).save(any(SedeLocalizacion.class));
    }

    @Test
    @DisplayName("Actualizar Sede exitosamente")
    void actualizarSedeExitosa() {
        UUID sedeId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                UUID.randomUUID(), "Viejo Nombre", "Vieja Calle", "1", null, "5360", 1L, 1L, 1L, true
        );
        when(sedeLocalizacionRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(sedeLocalizacionRepository.save(any(SedeLocalizacion.class))).thenAnswer(i -> i.getArgument(0));

        ActualizarSedeLocalizacionCommand cmd = new ActualizarSedeLocalizacionCommand(
                sedeId, "Nuevo Campus", "Nueva Calle", "2", "1", "5361", 1L, 1L, 1L, false
        );
        SedeLocalizacion actualizada = actualizarSedeLocalizacionService.actualizar(cmd);

        assertEquals("Nuevo Campus", actualizada.getNombre());
        assertEquals("Nueva Calle", actualizada.getCalle());
        assertFalse(actualizada.isEsPropia());
    }

    @Test
    @DisplayName("Cambiar estado Sede exitosamente")
    void cambiarEstadoSedeExitosa() {
        UUID sedeId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                UUID.randomUUID(), "Sede", "Calle", "1", null, "5360", 1L, 1L, 1L, true
        );
        when(sedeLocalizacionRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(sedeLocalizacionRepository.save(any(SedeLocalizacion.class))).thenAnswer(i -> i.getArgument(0));

        SedeLocalizacion desactivada = cambiarEstadoSedeLocalizacionService.cambiarEstado(sedeId, false);
        assertFalse(desactivada.isActivo());
    }

    @Test
    @DisplayName("Eliminar Sede exitosamente cuando no está asignada a carreras")
    void eliminarSedeExitosa() {
        UUID sedeId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                UUID.randomUUID(), "Sede", "Calle", "1", null, "5360", 1L, 1L, 1L, true
        );
        when(sedeLocalizacionRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(carreraRepository.existsBySedeId(sedeId)).thenReturn(false);

        eliminarSedeLocalizacionService.eliminar(sedeId);
        verify(sedeLocalizacionRepository).deleteById(sedeId);
    }

    @Test
    @DisplayName("Eliminar Sede falla si está asociada a alguna carrera")
    void eliminarSedeFallaPorCarreraAsociada() {
        UUID sedeId = UUID.randomUUID();
        SedeLocalizacion sede = SedeLocalizacion.registrar(
                UUID.randomUUID(), "Sede", "Calle", "1", null, "5360", 1L, 1L, 1L, true
        );
        when(sedeLocalizacionRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(carreraRepository.existsBySedeId(sedeId)).thenReturn(true);

        DomainValidationException ex = assertThrows(DomainValidationException.class,
                () -> eliminarSedeLocalizacionService.eliminar(sedeId));
        assertTrue(ex.getMessage().contains("asignada a una o más carreras"));
    }

    // ──────────────────────────── Carrera ────────────────────────────

    @Test
    @DisplayName("Registrar Carrera de Grado exitosamente con sedes asociadas")
    void registrarCarreraExitosa() {
        UUID uaId = UUID.randomUUID();
        UUID sedeId = UUID.randomUUID();

        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.of(mock(UnidadAcademica.class)));
        when(carreraRepository.existsByUnidadAcademicaIdAndNombre(uaId, "Ingeniería en Sistemas")).thenReturn(false);
        when(sedeLocalizacionRepository.findByIds(List.of(sedeId))).thenReturn(List.of(mock(SedeLocalizacion.class)));
        when(carreraRepository.save(any(Carrera.class))).thenAnswer(i -> i.getArgument(0));

        RegistrarCarreraCommand cmd = new RegistrarCarreraCommand(
                uaId,
                "Ingeniería en Sistemas",
                "ISI",
                "Ciencias Aplicadas",
                ModalidadCarrera.PRESENCIAL,
                EstadoCarrera.EN_FUNCIONAMIENTO,
                "Ingeniero/a en Sistemas",
                5,
                "Descripción de la carrera",
                List.of(sedeId)
        );

        Carrera carrera = registrarCarreraService.registrar(cmd);

        assertNotNull(carrera);
        assertEquals("Ingeniería en Sistemas", carrera.getNombre());
        assertEquals("GRADO", carrera.getNivel());
        assertEquals("Descripción de la carrera", carrera.getDescripcion());
        assertEquals(1, carrera.getSedesIds().size());
        verify(carreraRepository).save(any(Carrera.class));
    }

    @Test
    @DisplayName("Registrar Carrera falla si la sede asociada no existe")
    void registrarCarreraFallaSedeInexistente() {
        UUID uaId = UUID.randomUUID();
        UUID sedeId = UUID.randomUUID();

        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.of(mock(UnidadAcademica.class)));
        when(carreraRepository.existsByUnidadAcademicaIdAndNombre(uaId, "Ingeniería en Sistemas")).thenReturn(false);
        when(sedeLocalizacionRepository.findByIds(List.of(sedeId))).thenReturn(List.of());

        RegistrarCarreraCommand cmd = new RegistrarCarreraCommand(
                uaId,
                "Ingeniería en Sistemas",
                "ISI",
                "Ciencias Aplicadas",
                ModalidadCarrera.PRESENCIAL,
                EstadoCarrera.EN_FUNCIONAMIENTO,
                "Ingeniero/a en Sistemas",
                5,
                "Descripción",
                List.of(sedeId)
        );

        assertThrows(DomainValidationException.class, () -> registrarCarreraService.registrar(cmd));
    }

    @Test
    @DisplayName("Actualizar Carrera exitosamente")
    void actualizarCarreraExitosa() {
        UUID carreraId = UUID.randomUUID();
        UUID uaId = UUID.randomUUID();
        UUID nuevaSedeId = UUID.randomUUID();

        Carrera carrera = Carrera.registrar(
                uaId, "Viejo Nombre", "VN", "Ciencias Aplicadas",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Titulo", 5, "Vieja desc", List.of()
        );
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));
        when(carreraRepository.existsByUnidadAcademicaIdAndNombre(uaId, "Nuevo Nombre")).thenReturn(false);
        when(sedeLocalizacionRepository.findByIds(List.of(nuevaSedeId))).thenReturn(List.of(mock(SedeLocalizacion.class)));
        when(carreraRepository.save(any(Carrera.class))).thenAnswer(i -> i.getArgument(0));

        ActualizarCarreraCommand cmd = new ActualizarCarreraCommand(
                carreraId,
                "Nuevo Nombre",
                "NN",
                "Ciencias Exactas",
                ModalidadCarrera.DISTANCIA,
                EstadoCarrera.NUEVA,
                "Nuevo Titulo",
                4,
                "Nueva desc",
                false,
                List.of(nuevaSedeId)
        );

        Carrera actualizada = actualizarCarreraService.actualizar(cmd);

        assertEquals("Nuevo Nombre", actualizada.getNombre());
        assertEquals(ModalidadCarrera.DISTANCIA, actualizada.getModalidad());
        assertEquals(EstadoCarrera.NUEVA, actualizada.getEstado());
        assertEquals("Nueva desc", actualizada.getDescripcion());
        assertFalse(actualizada.isActivo());
        assertEquals(1, actualizada.getSedesIds().size());
    }

    @Test
    @DisplayName("Cambiar estado Carrera exitosamente")
    void cambiarEstadoCarreraExitosa() {
        UUID carreraId = UUID.randomUUID();
        Carrera carrera = Carrera.registrar(
                UUID.randomUUID(), "Carrera", "C", "Area",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Titulo", 5, "Desc", List.of()
        );
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));
        when(carreraRepository.save(any(Carrera.class))).thenAnswer(i -> i.getArgument(0));

        Carrera desactivada = cambiarEstadoCarreraService.cambiarEstado(carreraId, false);
        assertFalse(desactivada.isActivo());
    }

    @Test
    @DisplayName("Eliminar Carrera exitosamente")
    void eliminarCarreraExitosa() {
        UUID carreraId = UUID.randomUUID();
        Carrera carrera = Carrera.registrar(
                UUID.randomUUID(), "Carrera", "C", "Area",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Titulo", 5, "Desc", List.of()
        );
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carrera));

        eliminarCarreraService.eliminar(carreraId);
        verify(carreraRepository).deleteById(carreraId);
    }
}
