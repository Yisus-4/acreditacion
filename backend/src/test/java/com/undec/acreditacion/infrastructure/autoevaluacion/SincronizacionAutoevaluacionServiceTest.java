package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.application.ports.ModoClonacion;
import com.undec.acreditacion.application.ports.ResultadoSincronizacionDto;
import com.undec.acreditacion.application.ports.SeccionClonacion;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.SincronizacionAutoevaluacionService;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.CuerpoAcademicoMaestroJpaEntity;
import com.undec.acreditacion.infrastructure.datoscentrales.persistence.SpringDataCuerpoAcademicoMaestroRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SincronizacionAutoevaluacionServiceTest {

    @Mock
    private SpringDataProcesoAcreditacionRepository procesoRepository;
    @Mock
    private SpringDataConvocatoriaRepository convocatoriaRepository;
    @Mock
    private SpringDataConvocatoriaCarreraPlanRepository convocatoriaCarreraPlanRepository;
    @Mock
    private SpringDataPlanEstudioFt001Repository planEstudioRepository;
    @Mock
    private SpringDataActividadCurricularPlanRepository actividadRepository;
    @Mock
    private SpringDataCuerpoAcademicoMaestroRepository cuerpoAcademicoMaestroRepository;
    @Mock
    private SpringDataProcesoCuerpoAcademicoRepository procesoDocenteRepository;
    @Mock
    private SpringDataProcesoCuerpoAcademicoActividadRepository procesoDocenteActividadRepository;
    @Mock
    private SpringDataInmuebleRepository inmuebleRepository;
    @Mock
    private SpringDataEspacioFisicoRepository espacioFisicoRepository;
    @Mock
    private SpringDataEspacioFisicoActividadCurricularRepository espacioActividadRepository;
    @Mock
    private SpringDataAmbitoPracticaRepository ambitoPracticaRepository;
    @Mock
    private SpringDataAutoevaluacionCarreraAmbitoRepository autoevaluacionAmbitoRepository;

    @InjectMocks
    private SincronizacionAutoevaluacionService service;

    private UUID procesoId;
    private UUID carreraId;
    private ProcesoAcreditacionJpaEntity proceso;

    @BeforeEach
    void setUp() {
        procesoId = UUID.randomUUID();
        carreraId = UUID.randomUUID();
        proceso = new ProcesoAcreditacionJpaEntity();
        proceso.setId(procesoId);
        proceso.setCarreraId(carreraId);
    }

    @Test
    @DisplayName("Debe clonar planes, actividades, docentes e inmuebles/espacios con remapeo de FKs")
    void debeClonarDatosCentralesConRemapeoDeFks() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        // Master Plan & Course
        UUID masterPlanId = UUID.randomUUID();
        PlanEstudioFt001JpaEntity masterPlan = new PlanEstudioFt001JpaEntity();
        masterPlan.setId(masterPlanId);
        masterPlan.setNombrePlan("Plan 2024");
        masterPlan.setEsMaestro(true);

        ConvocatoriaCarreraPlanJpaEntity ccp = new ConvocatoriaCarreraPlanJpaEntity();
        ccp.setPlanEstudioMaestro(masterPlan);
        when(convocatoriaCarreraPlanRepository.findByProcesoIdAndSeleccionadoPorDeTrue(procesoId))
                .thenReturn(List.of(ccp));

        when(planEstudioRepository.findByProcesoIdAndOrigenMaestroId(procesoId, masterPlanId))
                .thenReturn(Optional.empty());
        when(planEstudioRepository.save(any(PlanEstudioFt001JpaEntity.class))).thenAnswer(invocation -> {
            PlanEstudioFt001JpaEntity arg = invocation.getArgument(0);
            if (arg.getId() == null) arg.setId(UUID.randomUUID());
            return arg;
        });

        UUID masterActId = UUID.randomUUID();
        ActividadCurricularPlanJpaEntity masterAct = new ActividadCurricularPlanJpaEntity();
        masterAct.setId(masterActId);
        masterAct.setNombre("Programación I");
        masterAct.setCodigo("PRG-101");
        when(actividadRepository.findByPlanEstudioIdOrderByNumeroOrdenAsc(masterPlanId))
                .thenReturn(List.of(masterAct));
        when(actividadRepository.findByPlanEstudioIdAndOrigenMaestroId(any(), eq(masterActId)))
                .thenReturn(Optional.empty());
        when(actividadRepository.save(any(ActividadCurricularPlanJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Master Docente
        CuerpoAcademicoMaestroJpaEntity masterDoc = new CuerpoAcademicoMaestroJpaEntity();
        masterDoc.setId(UUID.randomUUID());
        masterDoc.setDni("20123456");
        masterDoc.setApellido("Gonzalez");
        masterDoc.setNombre("Mario");
        when(cuerpoAcademicoMaestroRepository.findByCarreraId(carreraId)).thenReturn(List.of(masterDoc));
        when(procesoDocenteRepository.findByProcesoIdAndOrigenMaestroId(eq(procesoId), any())).thenReturn(Optional.empty());
        when(procesoDocenteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Master Inmueble & Espacios (Jerarquía con espacio padre y subespacio + inmueble no vinculado a carrera específica)
        InmuebleJpaEntity masterInmueble = new InmuebleJpaEntity();
        masterInmueble.setId(UUID.randomUUID());
        masterInmueble.setNombre("Sede Central");
        masterInmueble.setVinculaTodasCarreras(true);

        InmuebleJpaEntity masterInmuebleAdicional = new InmuebleJpaEntity();
        masterInmuebleAdicional.setId(UUID.randomUUID());
        masterInmuebleAdicional.setNombre("Edificio Anexo Sin Carrera");
        masterInmuebleAdicional.setVinculaTodasCarreras(false);

        when(inmuebleRepository.findMaestrosActivos()).thenReturn(List.of(masterInmueble, masterInmuebleAdicional));
        when(inmuebleRepository.findByProcesoIdAndOrigenMaestroId(eq(procesoId), any())).thenReturn(Optional.empty());
        when(inmuebleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EspacioFisicoJpaEntity masterEspacioPadre = new EspacioFisicoJpaEntity();
        masterEspacioPadre.setId(UUID.randomUUID());
        masterEspacioPadre.setDenominacion("Edificio Aulas");
        masterEspacioPadre.setInmueble(masterInmueble);
        masterEspacioPadre.setEspacioPadre(null);

        EspacioFisicoJpaEntity masterSubEspacio = new EspacioFisicoJpaEntity();
        masterSubEspacio.setId(UUID.randomUUID());
        masterSubEspacio.setDenominacion("Aula 101");
        masterSubEspacio.setInmueble(masterInmueble);
        masterSubEspacio.setEspacioPadre(masterEspacioPadre);

        when(espacioFisicoRepository.findByEsMaestroTrue()).thenReturn(List.of(masterEspacioPadre, masterSubEspacio));
        when(espacioFisicoRepository.findByProcesoIdAndOrigenMaestroId(eq(procesoId), any())).thenReturn(Optional.empty());
        when(espacioFisicoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Master Ambito de Practica vinculado a masterInmueble
        UUID masterAmbitoId = UUID.randomUUID();
        AmbitoPracticaJpaEntity masterAmbito = new AmbitoPracticaJpaEntity();
        masterAmbito.setId(masterAmbitoId);
        masterAmbito.setNumeroOrden(1);
        masterAmbito.setDenominacion("Laboratorio de Suelos");
        masterAmbito.setTipoCategoria("INTERNO");
        masterAmbito.setInmueble(masterInmueble);
        masterAmbito.setEsMaestro(true);
        masterAmbito.setActivo(true);

        when(ambitoPracticaRepository.findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc())
                .thenReturn(List.of(masterAmbito));
        when(ambitoPracticaRepository.findByProcesoIdAndOrigenMaestroId(procesoId, masterAmbitoId))
                .thenReturn(Optional.empty());
        when(ambitoPracticaRepository.save(any(AmbitoPracticaJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoSincronizacionDto res = service.copiarDatosCentrales(
                procesoId,
                ModoClonacion.INICIALIZAR_O_AGREGAR_FALTANTES,
                Set.of(SeccionClonacion.values())
        );

        assertNotNull(res);
        assertEquals(procesoId, res.procesoId());
        assertEquals(1, res.planesClonados());
        assertEquals(1, res.actividadesCurricularesClonadas());
        assertEquals(1, res.docentesClonados());
        assertEquals(2, res.inmueblesClonados());
        assertEquals(2, res.espaciosClonados());
        assertEquals(1, res.ambitosPracticaClonados());

        // Verificar que los espacios clonados referencien al inmueble clonado
        verify(espacioFisicoRepository, atLeast(2)).save(argThat(e ->
                !Boolean.TRUE.equals(e.getEsMaestro()) &&
                procesoId.equals(e.getProcesoId()) &&
                e.getInmueble() != null &&
                !Boolean.TRUE.equals(e.getInmueble().getEsMaestro())
        ));

        // Verificar que el ambito clonado este aislado por proceso, remapeado al inmueble clonado y sin vinculaciones forzadas
        verify(ambitoPracticaRepository).save(argThat(a ->
                !Boolean.TRUE.equals(a.getEsMaestro()) &&
                procesoId.equals(a.getProcesoId()) &&
                masterAmbitoId.equals(a.getOrigenMaestroId()) &&
                a.getInmueble() != null &&
                !Boolean.TRUE.equals(a.getInmueble().getEsMaestro()) &&
                masterInmueble.getId().equals(a.getInmueble().getOrigenMaestroId())
        ));
        verify(autoevaluacionAmbitoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modo REEMPLAZAR_COPIA debe limpiar vinculaciones y copias previas de ambitos antes de clonar")
    void debeLimpiarYReclonarAmbitosEnModoReemplazar() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        AmbitoPracticaJpaEntity viejaCopia = new AmbitoPracticaJpaEntity();
        viejaCopia.setId(UUID.randomUUID());
        viejaCopia.setProcesoId(procesoId);
        viejaCopia.setEsMaestro(false);

        AutoevaluacionCarreraAmbitoJpaEntity viejoVinculo = new AutoevaluacionCarreraAmbitoJpaEntity();
        viejoVinculo.setId(UUID.randomUUID());
        viejoVinculo.setProcesoId(procesoId);
        viejoVinculo.setAmbito(viejaCopia);

        when(autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId))
                .thenReturn(List.of(viejoVinculo));
        when(ambitoPracticaRepository.findByProcesoId(procesoId))
                .thenReturn(List.of(viejaCopia));

        UUID masterAmbitoId = UUID.randomUUID();
        AmbitoPracticaJpaEntity masterAmbito = new AmbitoPracticaJpaEntity();
        masterAmbito.setId(masterAmbitoId);
        masterAmbito.setDenominacion("Campo Experimental");
        masterAmbito.setTipoCategoria("UNIDAD_DEMOSTRATIVA");
        masterAmbito.setEsMaestro(true);
        masterAmbito.setActivo(true);

        when(ambitoPracticaRepository.findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc())
                .thenReturn(List.of(masterAmbito));
        when(ambitoPracticaRepository.findByProcesoIdAndOrigenMaestroId(procesoId, masterAmbitoId))
                .thenReturn(Optional.empty());
        when(ambitoPracticaRepository.save(any(AmbitoPracticaJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoSincronizacionDto res = service.copiarDatosCentrales(
                procesoId,
                ModoClonacion.REEMPLAZAR_COPIA,
                Set.of(SeccionClonacion.AMBITOS_PRACTICA)
        );

        assertNotNull(res);
        assertEquals(1, res.ambitosPracticaClonados());
        verify(autoevaluacionAmbitoRepository).deleteAll(List.of(viejoVinculo));
        verify(ambitoPracticaRepository).deleteAll(List.of(viejaCopia));
        verify(ambitoPracticaRepository).save(argThat(a ->
                !Boolean.TRUE.equals(a.getEsMaestro()) &&
                procesoId.equals(a.getProcesoId()) &&
                masterAmbitoId.equals(a.getOrigenMaestroId())
        ));
    }

    @Test
    @DisplayName("Modo REEMPLAZAR_COPIA debe limpiar copias previas antes de clonar")
    void debeLimpiarCopiasPreviasEnModoReemplazar() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(inmuebleRepository.findMaestrosActivos()).thenReturn(Collections.emptyList());
        when(espacioFisicoRepository.findByEsMaestroTrue()).thenReturn(Collections.emptyList());

        EspacioFisicoJpaEntity viejoEspacio = new EspacioFisicoJpaEntity();
        viejoEspacio.setId(UUID.randomUUID());
        when(espacioFisicoRepository.findByProcesoId(procesoId)).thenReturn(List.of(viejoEspacio));

        ResultadoSincronizacionDto res = service.copiarDatosCentrales(
                procesoId,
                ModoClonacion.REEMPLAZAR_COPIA,
                Set.of(SeccionClonacion.INFRAESTRUCTURA_Y_ESPACIOS)
        );

        assertNotNull(res);
        verify(espacioActividadRepository).deleteByEspacioFisicoId(viejoEspacio.getId());
        verify(espacioFisicoRepository).deleteAll(List.of(viejoEspacio));
    }

    @Test
    @DisplayName("Modo INICIALIZAR_O_AGREGAR_FALTANTES limpia vinculaciones huérfanas al maestro y deja clones vacíos")
    void debeLimpiarVinculacionesHuerfanasAlMaestroYDejarClonesVaciosAlCopiarAmbitos() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        AmbitoPracticaJpaEntity maestro = new AmbitoPracticaJpaEntity();
        maestro.setId(UUID.randomUUID());
        maestro.setDenominacion("Gabinete de Enfermería");
        maestro.setTipoCategoria("INTERNO");
        maestro.setEsMaestro(true);
        maestro.setProcesoId(null);
        maestro.setActivo(true);

        AutoevaluacionCarreraAmbitoJpaEntity huerfana = new AutoevaluacionCarreraAmbitoJpaEntity();
        huerfana.setId(UUID.randomUUID());
        huerfana.setProcesoId(procesoId);
        huerfana.setAmbito(maestro);
        huerfana.setUtilizadoPorCarrera(true);
        huerfana.setAsignaturasVinculadas("analisis 1");

        AmbitoPracticaJpaEntity clonExistente = new AmbitoPracticaJpaEntity();
        clonExistente.setId(UUID.randomUUID());
        clonExistente.setDenominacion("Campo Clon");
        clonExistente.setTipoCategoria("UNIDAD_DEMOSTRATIVA");
        clonExistente.setEsMaestro(false);
        clonExistente.setProcesoId(procesoId);
        clonExistente.setActivo(true);

        AutoevaluacionCarreraAmbitoJpaEntity vinculoValido = new AutoevaluacionCarreraAmbitoJpaEntity();
        vinculoValido.setId(UUID.randomUUID());
        vinculoValido.setProcesoId(procesoId);
        vinculoValido.setAmbito(clonExistente);
        vinculoValido.setUtilizadoPorCarrera(true);

        when(autoevaluacionAmbitoRepository.findByProcesoIdOrderByAmbitoNumeroOrdenAsc(procesoId))
                .thenReturn(List.of(huerfana, vinculoValido));
        when(ambitoPracticaRepository.findByEsMaestroTrueAndActivoTrueOrderByNumeroOrdenAsc())
                .thenReturn(List.of(maestro));
        when(ambitoPracticaRepository.findByProcesoIdAndOrigenMaestroId(procesoId, maestro.getId()))
                .thenReturn(Optional.empty());
        when(ambitoPracticaRepository.save(any(AmbitoPracticaJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoSincronizacionDto res = service.copiarDatosCentrales(
                procesoId,
                ModoClonacion.INICIALIZAR_O_AGREGAR_FALTANTES,
                Set.of(SeccionClonacion.AMBITOS_PRACTICA)
        );

        assertEquals(1, res.ambitosPracticaClonados());
        verify(autoevaluacionAmbitoRepository).deleteAll(List.of(huerfana));
        verify(autoevaluacionAmbitoRepository, never()).save(any());
        verify(ambitoPracticaRepository).save(argThat(a ->
                !Boolean.TRUE.equals(a.getEsMaestro())
                        && procesoId.equals(a.getProcesoId())
                        && maestro.getId().equals(a.getOrigenMaestroId())
        ));
    }

    @Test
    @DisplayName("Modo INICIALIZAR_O_AGREGAR_FALTANTES reactiva copias de inmuebles y espacios previamente desactivadas")
    void debeReactivarCopiasInactivasDeInmueblesYEspaciosAlCopiar() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        UUID masterInmId = UUID.randomUUID();
        InmuebleJpaEntity masterInm = new InmuebleJpaEntity();
        masterInm.setId(masterInmId);
        masterInm.setNombre("Sede Central");
        masterInm.setEsMaestro(true);
        masterInm.setActivo(true);

        InmuebleJpaEntity cloneInactivo = new InmuebleJpaEntity();
        cloneInactivo.setId(UUID.randomUUID());
        cloneInactivo.setNombre("Sede Central");
        cloneInactivo.setEsMaestro(false);
        cloneInactivo.setProcesoId(procesoId);
        cloneInactivo.setOrigenMaestroId(masterInmId);
        cloneInactivo.setActivo(false);

        UUID masterEspId = UUID.randomUUID();
        EspacioFisicoJpaEntity masterEsp = new EspacioFisicoJpaEntity();
        masterEsp.setId(masterEspId);
        masterEsp.setDenominacion("Aula 1");
        masterEsp.setInmueble(masterInm);
        masterEsp.setEsMaestro(true);
        masterEsp.setActivo(true);

        EspacioFisicoJpaEntity espInactivo = new EspacioFisicoJpaEntity();
        espInactivo.setId(UUID.randomUUID());
        espInactivo.setDenominacion("Aula 1");
        espInactivo.setInmueble(cloneInactivo);
        espInactivo.setEsMaestro(false);
        espInactivo.setProcesoId(procesoId);
        espInactivo.setOrigenMaestroId(masterEspId);
        espInactivo.setActivo(false);

        when(inmuebleRepository.findMaestrosActivos()).thenReturn(List.of(masterInm));
        when(inmuebleRepository.findByProcesoIdAndOrigenMaestroId(procesoId, masterInmId))
                .thenReturn(Optional.of(cloneInactivo));
        when(inmuebleRepository.save(any(InmuebleJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(espacioFisicoRepository.findByEsMaestroTrue()).thenReturn(List.of(masterEsp));
        when(espacioFisicoRepository.findByProcesoIdAndOrigenMaestroId(procesoId, masterEspId))
                .thenReturn(Optional.of(espInactivo));
        when(espacioFisicoRepository.save(any(EspacioFisicoJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoSincronizacionDto res = service.copiarDatosCentrales(
                procesoId,
                ModoClonacion.INICIALIZAR_O_AGREGAR_FALTANTES,
                Set.of(SeccionClonacion.INFRAESTRUCTURA_Y_ESPACIOS)
        );

        assertEquals(1, res.inmueblesClonados());
        assertEquals(1, res.espaciosClonados());
        assertTrue(cloneInactivo.getActivo());
        assertTrue(espInactivo.getActivo());
    }
}
