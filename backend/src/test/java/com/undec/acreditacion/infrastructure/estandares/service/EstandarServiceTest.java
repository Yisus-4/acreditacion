package com.undec.acreditacion.infrastructure.estandares.service;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.ModoSeccion311;
import com.undec.acreditacion.domain.entities.ModoSeccion312;
import com.undec.acreditacion.domain.entities.Role;
import com.undec.acreditacion.domain.entities.TipoEvaluacionEstandar22;
import com.undec.acreditacion.domain.entities.TipoSeccionCargaHoraria;
import com.undec.acreditacion.domain.entities.TipoSeccionEstandar22;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataPlanEstudioFt001Repository;
import com.undec.acreditacion.infrastructure.estandares.dto.EstandarDetalleDto;
import com.undec.acreditacion.infrastructure.estandares.dto.GuardarEstandarRequest;
import com.undec.acreditacion.infrastructure.estandares.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.CarreraJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataCarreraRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.UnidadAcademicaJpaEntity;
import com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstandarServiceTest {

    @Mock private SpringDataEstandarRepository estandarRepository;
    @Mock private SpringDataEstandarEscalaEvaluacionRepository escalaRepository;
    @Mock private SpringDataEstandarGrupo22Repository grupoRepository;
    @Mock private SpringDataEstandarItem22Repository itemRepository;
    @Mock private SpringDataEstandarAreaCargaHorariaRepository areaRepository;
    @Mock private SpringDataCarreraRepository carreraRepository;
    @Mock private SpringDataPlanEstudioFt001Repository planRepository;
    @Mock private AuthenticatedUserProvider authenticatedUserProvider;

    private EstandarService service;

    @BeforeEach
    void setUp() {
        service = new EstandarService(
                estandarRepository,
                escalaRepository,
                grupoRepository,
                itemRepository,
                areaRepository,
                carreraRepository,
                planRepository,
                authenticatedUserProvider
        );
    }

    @Test
    @DisplayName("RN-01: crear estándar VIGENTE degrada el estándar vigente anterior a NO_VIGENTE con flush")
    void crearVigenteDegradaAnterior() {
        UUID uaId = UUID.randomUUID();
        CarreraJpaEntity carrera = buildCarrera(uaId);
        EstandarJpaEntity anterior = buildEstandar(UUID.randomUUID(), carrera, "Estándar 2020", EstadoVigenciaEstandar.VIGENTE);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(userWithRole("AEA", null)));
        when(authenticatedUserProvider.getAuthenticatedUserIdOrNull()).thenReturn(UUID.randomUUID());
        when(carreraRepository.findById(carrera.getId())).thenReturn(Optional.of(carrera));
        when(estandarRepository.findByCarreraIdAndEstadoVigencia(carrera.getId(), EstadoVigenciaEstandar.VIGENTE))
                .thenReturn(Optional.of(anterior));
        when(estandarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(grupoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildren();

        GuardarEstandarRequest req = buildRequest(carrera.getId(), "Estándar 2026");
        EstandarDetalleDto res = service.crear(req);

        assertEquals(EstadoVigenciaEstandar.NO_VIGENTE, anterior.getEstadoVigencia());
        assertEquals(EstadoVigenciaEstandar.VIGENTE, res.estadoVigencia());
        InOrder inOrder = inOrder(estandarRepository);
        inOrder.verify(estandarRepository).save(anterior);
        inOrder.verify(estandarRepository).flush();
        inOrder.verify(estandarRepository).save(argThat(e -> !e.getId().equals(anterior.getId()) && e.getEstadoVigencia() == EstadoVigenciaEstandar.VIGENTE));
    }

    @Test
    @DisplayName("RN-02: impide editar estándar NO_VIGENTE o estándar con planes asociados")
    void impideEditarNoVigenteOConPlanesAsociados() {
        UUID uaId = UUID.randomUUID();
        CarreraJpaEntity carrera = buildCarrera(uaId);
        EstandarJpaEntity noVigente = buildEstandar(UUID.randomUUID(), carrera, "Histórico", EstadoVigenciaEstandar.NO_VIGENTE);
        EstandarJpaEntity conPlanes = buildEstandar(UUID.randomUUID(), carrera, "En Uso", EstadoVigenciaEstandar.VIGENTE);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(userWithRole("AEA", null)));
        when(estandarRepository.findById(noVigente.getId())).thenReturn(Optional.of(noVigente));
        when(estandarRepository.findById(conPlanes.getId())).thenReturn(Optional.of(conPlanes));
        when(planRepository.countByEstandarId(conPlanes.getId())).thenReturn(2L);

        assertThrows(DomainValidationException.class,
                () -> service.actualizar(noVigente.getId(), buildRequest(carrera.getId(), "Mod")));
        assertThrows(DomainValidationException.class,
                () -> service.actualizar(conPlanes.getId(), buildRequest(carrera.getId(), "Mod")));
    }

    @Test
    @DisplayName("crearNuevaVersion clona escalas, grupos (con habilitado), ítems y áreas, degrada anterior y versiona el nombre")
    void crearNuevaVersionClonaEstructuraYVersionaNombre() {
        UUID uaId = UUID.randomUUID();
        CarreraJpaEntity carrera = buildCarrera(uaId);
        EstandarJpaEntity origen = buildEstandar(UUID.randomUUID(), carrera, "Estándar CONEAU (v2)", EstadoVigenciaEstandar.VIGENTE);

        EstandarEscalaEvaluacionJpaEntity esc = new EstandarEscalaEvaluacionJpaEntity();
        esc.setId(UUID.randomUUID());
        esc.setEstandar(origen);
        esc.setOrden(1);
        esc.setCodigo("APRENDE");
        esc.setEtiqueta("Aprende");
        esc.setEsValorPorDefecto(false);

        EstandarGrupo22JpaEntity g1 = new EstandarGrupo22JpaEntity();
        g1.setId(UUID.randomUUID());
        g1.setEstandar(origen);
        g1.setOrden(1);
        g1.setCodigo("BASICA");
        g1.setTitulo("Formación Básica");
        g1.setHabilitado(true);

        EstandarGrupo22JpaEntity g2 = new EstandarGrupo22JpaEntity();
        g2.setId(UUID.randomUUID());
        g2.setEstandar(origen);
        g2.setOrden(2);
        g2.setCodigo("APLICADA");
        g2.setTitulo("Formación Aplicada");
        g2.setHabilitado(false);

        EstandarItem22JpaEntity item1 = new EstandarItem22JpaEntity();
        item1.setId(UUID.randomUUID());
        item1.setGrupo(g1);
        item1.setOrden(1);
        item1.setEnunciado("Álgebra y análisis.");

        EstandarAreaCargaHorariaJpaEntity area1 = new EstandarAreaCargaHorariaJpaEntity();
        area1.setId(UUID.randomUUID());
        area1.setEstandar(origen);
        area1.setTipoSeccion(TipoSeccionCargaHoraria.CARGA_TOTAL_311);
        area1.setOrden(1);
        area1.setCodigo("BASICA");
        area1.setNombre("Formación Básica");
        area1.setHabilitada(false);

        EstandarAreaCargaHorariaJpaEntity area2 = new EstandarAreaCargaHorariaJpaEntity();
        area2.setId(UUID.randomUUID());
        area2.setEstandar(origen);
        area2.setTipoSeccion(TipoSeccionCargaHoraria.PRACTICA_312);
        area2.setOrden(1);
        area2.setCodigo("BASICA");
        area2.setNombre("Formación Básica");
        area2.setHabilitada(true);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(userWithRole("AEA", null)));
        when(authenticatedUserProvider.getAuthenticatedUserIdOrNull()).thenReturn(UUID.randomUUID());
        when(estandarRepository.findById(origen.getId())).thenReturn(Optional.of(origen));
        when(estandarRepository.findByCarreraIdAndEstadoVigencia(carrera.getId(), EstadoVigenciaEstandar.VIGENTE))
                .thenReturn(Optional.of(origen));
        when(estandarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(grupoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // First call returns origin children; second call (for newly saved standard) returns cloned
        when(escalaRepository.findByEstandarIdOrderByOrdenAsc(origen.getId())).thenReturn(List.of(esc));
        when(grupoRepository.findByEstandarIdOrderByOrdenAsc(origen.getId())).thenReturn(List.of(g1, g2));
        when(itemRepository.findByGrupoIdInOrderByOrdenAsc(any())).thenReturn(List.of(item1));
        when(areaRepository.findByEstandarIdOrderByOrdenAsc(origen.getId())).thenReturn(List.of(area1, area2));
        when(escalaRepository.findByEstandarIdOrderByOrdenAsc(argThat(id -> !id.equals(origen.getId())))).thenReturn(List.of(esc));
        when(grupoRepository.findByEstandarIdOrderByOrdenAsc(argThat(id -> !id.equals(origen.getId())))).thenReturn(List.of(g1, g2));
        when(areaRepository.findByEstandarIdOrderByOrdenAsc(argThat(id -> !id.equals(origen.getId())))).thenReturn(List.of(area1, area2));

        EstandarDetalleDto nueva = service.crearNuevaVersion(origen.getId());

        assertEquals("Estándar CONEAU (v3)", nueva.nombre());
        assertEquals(EstadoVigenciaEstandar.NO_VIGENTE, origen.getEstadoVigencia());
        verify(escalaRepository).saveAll(argThat(col -> ((Collection<?>) col).size() == 1));
        verify(grupoRepository, times(2)).save(any());
        verify(itemRepository).saveAll(argThat(col -> ((Collection<?>) col).size() == 1));
        verify(areaRepository).saveAll(argThat(col -> {
            List<EstandarAreaCargaHorariaJpaEntity> list = new ArrayList<>();
            col.forEach(list::add);
            return list.size() == 2
                    && !list.get(0).getHabilitada()
                    && list.get(0).getTipoSeccion() == TipoSeccionCargaHoraria.CARGA_TOTAL_311
                    && list.get(1).getTipoSeccion() == TipoSeccionCargaHoraria.PRACTICA_312
                    && "BASICA".equals(list.get(0).getCodigo())
                    && "BASICA".equals(list.get(1).getCodigo());
        }));
    }

    @Test
    @DisplayName("Permisos: AEA libre; DIRECTOR_ESCUELA solo su UA (403 en otra UA); DOCENTE 403")
    void validaPermisosPorRolYUnidadAcademica() {
        UUID uaPropia = UUID.randomUUID();
        UUID otraUa = UUID.randomUUID();
        CarreraJpaEntity carreraPropia = buildCarrera(uaPropia);
        CarreraJpaEntity carreraAjena = buildCarrera(otraUa);

        when(carreraRepository.findById(carreraPropia.getId())).thenReturn(Optional.of(carreraPropia));
        when(carreraRepository.findById(carreraAjena.getId())).thenReturn(Optional.of(carreraAjena));
        when(estandarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(grupoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildren();

        // DIRECTOR_ESCUELA on own UA succeeds
        when(authenticatedUserProvider.getAuthenticatedUser())
                .thenReturn(Optional.of(userWithRole("DIRECTOR_ESCUELA", uaPropia)));
        assertDoesNotThrow(() -> service.crear(buildRequest(carreraPropia.getId(), "Propia")));

        // DIRECTOR_ESCUELA on another UA fails with 403
        when(authenticatedUserProvider.getAuthenticatedUser())
                .thenReturn(Optional.of(userWithRole("DIRECTOR_ESCUELA", uaPropia)));
        ResponseStatusException exOtherUa = assertThrows(ResponseStatusException.class,
                () -> service.crear(buildRequest(carreraAjena.getId(), "Ajena")));
        assertEquals(HttpStatus.FORBIDDEN, exOtherUa.getStatusCode());

        // DOCENTE fails with 403
        when(authenticatedUserProvider.getAuthenticatedUser())
                .thenReturn(Optional.of(userWithRole("DOCENTE", uaPropia)));
        ResponseStatusException exDocente = assertThrows(ResponseStatusException.class,
                () -> service.crear(buildRequest(carreraPropia.getId(), "Docente")));
        assertEquals(HttpStatus.FORBIDDEN, exDocente.getStatusCode());

        // AEA succeeds even on another UA
        when(authenticatedUserProvider.getAuthenticatedUser())
                .thenReturn(Optional.of(userWithRole("AEA", uaPropia)));
        assertDoesNotThrow(() -> service.crear(buildRequest(carreraAjena.getId(), "AEA")));
    }

    @Test
    @DisplayName("Sección 3: permite activar/desactivar 3.1.1 y 3.1.2 independientemente y guarda horas mínimas globales de carrera")
    void seccion3ModosYHorasMinimasGlobales() {
        UUID uaId = UUID.randomUUID();
        CarreraJpaEntity carrera = buildCarrera(uaId);

        when(authenticatedUserProvider.getAuthenticatedUser()).thenReturn(Optional.of(userWithRole("AEA", null)));
        when(authenticatedUserProvider.getAuthenticatedUserIdOrNull()).thenReturn(UUID.randomUUID());
        when(carreraRepository.findById(carrera.getId())).thenReturn(Optional.of(carrera));
        when(estandarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildren();

        // 1. Permite dar de baja ambas subsecciones (3.1.1 y 3.1.2)
        GuardarEstandarRequest sinSeccion3 = new GuardarEstandarRequest(
                carrera.getId(), "Sin S3", "RM 1/26", 2026, EstadoVigenciaEstandar.VIGENTE,
                TipoSeccionEstandar22.NINGUNA, TipoEvaluacionEstandar22.NINGUNA, null, null,
                false, ModoSeccion311.BLOQUES_CURRICULARES, null,
                false, ModoSeccion312.MODALIDADES_PRACTICAS, null,
                List.of(), List.of(), List.of()
        );
        EstandarDetalleDto resSinS3 = service.crear(sinSeccion3);
        assertFalse(resSinS3.habilitar311());
        assertFalse(resSinS3.habilitar312());
        assertEquals(ModoSeccion311.BLOQUES_CURRICULARES, resSinS3.modo311());
        assertEquals(ModoSeccion312.MODALIDADES_PRACTICAS, resSinS3.modo312());

        // 2. Rechaza horas mínimas negativas
        GuardarEstandarRequest minNegativo = new GuardarEstandarRequest(
                carrera.getId(), "Negativo", "RM 2/26", 2026, EstadoVigenciaEstandar.VIGENTE,
                TipoSeccionEstandar22.NINGUNA, TipoEvaluacionEstandar22.NINGUNA, null, null,
                true, ModoSeccion311.BLOQUES_CURRICULARES, -100,
                false, ModoSeccion312.MODALIDADES_PRACTICAS, null,
                List.of(), List.of(),
                List.of(new GuardarEstandarRequest.AreaCargaHorariaRequest(
                        TipoSeccionCargaHoraria.CARGA_TOTAL_311, 1, "CBI", "Ciencias Básicas", "Ciencias Básicas", true, 750))
        );
        assertThrows(DomainValidationException.class, () -> service.crear(minNegativo));

        // 3. Guarda horas mínimas globales en cabecera y en cada fila habilitada
        GuardarEstandarRequest conMinimos = new GuardarEstandarRequest(
                carrera.getId(), "Ingeniería RM 1254", "RM 1254/18", 2026, EstadoVigenciaEstandar.VIGENTE,
                TipoSeccionEstandar22.NINGUNA, TipoEvaluacionEstandar22.NINGUNA, null, null,
                true, ModoSeccion311.BLOQUES_CURRICULARES, 3600,
                true, ModoSeccion312.MODALIDADES_PRACTICAS, 750,
                List.of(), List.of(),
                List.of(
                        new GuardarEstandarRequest.AreaCargaHorariaRequest(
                                TipoSeccionCargaHoraria.CARGA_TOTAL_311, 1, "CBI", "Ciencias Básicas", "Ciencias Básicas", true, 750),
                        new GuardarEstandarRequest.AreaCargaHorariaRequest(
                                TipoSeccionCargaHoraria.PRACTICA_312, 1, "PPS", "PPS", "PPS", true, 200)
                )
        );
        EstandarDetalleDto resMin = service.crear(conMinimos);
        assertTrue(resMin.habilitar311());
        assertEquals(3600, resMin.horasMinimasTotal311());
        assertEquals(750, resMin.horasMinimasTotal312());
        verify(areaRepository).saveAll(argThat(col -> {
            List<EstandarAreaCargaHorariaJpaEntity> list = new ArrayList<>();
            col.forEach(list::add);
            return list.size() == 2
                    && Integer.valueOf(750).equals(list.get(0).getHorasMinimasCarrera())
                    && Integer.valueOf(200).equals(list.get(1).getHorasMinimasCarrera());
        }));
    }

    private void stubEmptyChildren() {
        when(escalaRepository.findByEstandarIdOrderByOrdenAsc(any())).thenReturn(List.of());
        when(grupoRepository.findByEstandarIdOrderByOrdenAsc(any())).thenReturn(List.of());
        when(areaRepository.findByEstandarIdOrderByOrdenAsc(any())).thenReturn(List.of());
    }

    private CarreraJpaEntity buildCarrera(UUID uaId) {
        UnidadAcademicaJpaEntity ua = new UnidadAcademicaJpaEntity();
        ua.setId(uaId);
        ua.setNombre("Departamento");
        CarreraJpaEntity c = new CarreraJpaEntity();
        c.setId(UUID.randomUUID());
        c.setNombre("Ingeniería");
        c.setSigla("ING");
        c.setUnidadAcademica(ua);
        return c;
    }

    private EstandarJpaEntity buildEstandar(UUID id, CarreraJpaEntity c, String nombre, EstadoVigenciaEstandar estado) {
        EstandarJpaEntity e = new EstandarJpaEntity();
        e.setId(id);
        e.setCarrera(c);
        e.setNombre(nombre);
        e.setResolucionMinisterial("RM 123/26");
        e.setAnioVigencia(2026);
        e.setEstadoVigencia(estado);
        e.setTipoSeccion22(TipoSeccionEstandar22.COMPETENCIAS);
        e.setTipoEvaluacion22(TipoEvaluacionEstandar22.MATRIZ_MULTIPLE);
        e.setCreadoEn(Instant.now());
        e.setActualizadoEn(Instant.now());
        return e;
    }

    private GuardarEstandarRequest buildRequest(UUID carreraId, String nombre) {
        return new GuardarEstandarRequest(
                carreraId,
                nombre,
                "RM 123/26",
                2026,
                EstadoVigenciaEstandar.VIGENTE,
                TipoSeccionEstandar22.COMPETENCIAS,
                TipoEvaluacionEstandar22.MATRIZ_MULTIPLE,
                "Instrucciones",
                null,
                List.of(new GuardarEstandarRequest.EscalaItemRequest(1, "APRENDE", "Aprende", "Desc", false)),
                List.of(new GuardarEstandarRequest.Grupo22Request(
                        1, "BASICA", "Formación Básica", null, true,
                        List.of(new GuardarEstandarRequest.Item22Request(1, "Contenido 1")))),
                List.of(
                        new GuardarEstandarRequest.AreaCargaHorariaRequest(
                                TipoSeccionCargaHoraria.CARGA_TOTAL_311, 1, "BASICA", "Formación Básica", "Formación Básica", true),
                        new GuardarEstandarRequest.AreaCargaHorariaRequest(
                                TipoSeccionCargaHoraria.PRACTICA_312, 1, "BASICA", "Formación Básica", "Formación Básica", true)
                )
        );
    }

    private User userWithRole(String code, UUID uaId) {
        Role role = Role.system(UUID.randomUUID(), code, code, code);
        return User.rehydrate(UUID.randomUUID(), "user_" + code.toLowerCase(), code.toLowerCase() + "@undec.edu.ar",
                "hash", true, false, uaId, Set.of(role));
    }
}
