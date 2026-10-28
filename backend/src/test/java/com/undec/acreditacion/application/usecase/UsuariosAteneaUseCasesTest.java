package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.GuardarUsuarioPermisoAteneaInputDto;
import com.undec.acreditacion.application.output.RegistroUsuariosAteneaDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UsuariosAteneaUseCasesTest {

    private RegistroUsuariosAteneaRepositoryPort registroUsuariosRepo;
    private RegistroConformacionCaRepositoryPort registroCaRepo;
    private ProcesoAcreditacionRepositoryPort procesoRepo;
    private CarreraRepositoryPort carreraRepo;
    private RegistroConvocatoriaRepositoryPort convocatoriaRepo;
    private PlanTrabajoRepositoryPort planTrabajoRepo;
    private DocumentStoragePort storagePort;
    private DocumentoAcreditacionRepositoryPort documentoRepo;

    private ConsultarRegistroUsuariosAteneaService consultarService;
    private GuardarRegistroUsuariosAteneaService guardarService;
    private FinalizarRegistroUsuariosAteneaService finalizarService;

    private UUID procesoId;
    private UUID carreraId;
    private UUID usuarioAeaId;

    @BeforeEach
    void setUp() {
        registroUsuariosRepo = mock(RegistroUsuariosAteneaRepositoryPort.class);
        registroCaRepo = mock(RegistroConformacionCaRepositoryPort.class);
        procesoRepo = mock(ProcesoAcreditacionRepositoryPort.class);
        carreraRepo = mock(CarreraRepositoryPort.class);
        convocatoriaRepo = mock(RegistroConvocatoriaRepositoryPort.class);
        planTrabajoRepo = mock(PlanTrabajoRepositoryPort.class);
        storagePort = mock(DocumentStoragePort.class);
        documentoRepo = mock(DocumentoAcreditacionRepositoryPort.class);

        consultarService = new ConsultarRegistroUsuariosAteneaService(
                registroUsuariosRepo, registroCaRepo, procesoRepo
        );
        guardarService = new GuardarRegistroUsuariosAteneaService(registroUsuariosRepo);
        finalizarService = new FinalizarRegistroUsuariosAteneaService(
                registroUsuariosRepo, procesoRepo, carreraRepo, convocatoriaRepo,
                planTrabajoRepo, storagePort, documentoRepo
        );

        procesoId = UUID.randomUUID();
        carreraId = UUID.randomUUID();
        usuarioAeaId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Consultar: Debe retornar existente si ya fue inicializado")
    void debeRetornarExistente() {
        RegistroUsuariosAtenea existente = RegistroUsuariosAtenea.iniciar(procesoId, usuarioAeaId, LocalDate.now());
        when(registroUsuariosRepo.findByProcesoId(procesoId)).thenReturn(Optional.of(existente));

        RegistroUsuariosAteneaDto dto = consultarService.consultarPorProcesoId(procesoId);

        assertNotNull(dto);
        assertEquals(existente.getId(), dto.id());
        verify(registroUsuariosRepo, never()).save(any());
    }

    @Test
    @DisplayName("Consultar: Debe pre-poblar AEA, SGI y miembros de CA cuando se inicializa por primera vez")
    void debePrePoblarUsuariosAlInicializar() {
        when(registroUsuariosRepo.findByProcesoId(procesoId)).thenReturn(Optional.empty());

        ProcesoAcreditacion proceso = ProcesoAcreditacion.rehydrate(
                procesoId, carreraId, "PROC-001", "Acreditación Agronomía",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, EstadoProcesoAcreditacion.EN_AUTOEVALUACION,
                usuarioAeaId, Instant.now(), Instant.now()
        );
        when(procesoRepo.findById(procesoId)).thenReturn(Optional.of(proceso));

        MiembroComisionAutoevaluacion m1 = new MiembroComisionAutoevaluacion(
                UUID.randomUUID(), UUID.randomUUID(), "Salerno", "Julio", "Luis",
                "20-08386710-9", "20-08386710-9", "jsalerno@undec.edu.ar", "salerno@gmail.com",
                "Director de Carrera", "3825000000", RolClaustroCa.DOCENTE,
                true, false, true, false, 1
        );
        RegistroConformacionCa ca = new RegistroConformacionCa(
                UUID.randomUUID(), procesoId, EstadoRegistroCa.APROBADA, false,
                Instant.now(), usuarioAeaId, LocalDate.now().plusDays(10), Instant.now(), usuarioAeaId,
                Instant.now(), usuarioAeaId, null, null, null, null, "minio/ca.pdf",
                Instant.now(), Instant.now(), List.of(m1)
        );
        when(registroCaRepo.findByProcesoId(procesoId)).thenReturn(Optional.of(ca));

        when(registroUsuariosRepo.save(any(RegistroUsuariosAtenea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistroUsuariosAteneaDto dto = consultarService.consultarPorProcesoId(procesoId);

        assertNotNull(dto);
        assertEquals(EstadoRegistroUsuariosAtenea.EN_DEFINICION, dto.estado());
        assertFalse(dto.finalizado());
        assertEquals(3, dto.usuarios().size()); // 1 AEA + 1 SGI + 1 CA

        assertTrue(dto.usuarios().stream().anyMatch(u -> u.grupoUsuario() == GrupoUsuarioAtenea.AEA));
        assertTrue(dto.usuarios().stream().anyMatch(u -> u.grupoUsuario() == GrupoUsuarioAtenea.SGI));
        assertTrue(dto.usuarios().stream().anyMatch(u -> u.grupoUsuario() == GrupoUsuarioAtenea.COMISION_AUTOEVALUACION
                && "Salerno".equals(u.apellido())));
    }

    @Test
    @DisplayName("Guardar: Debe actualizar la lista de usuarios y persistir")
    void debeGuardarUsuariosActualizados() {
        RegistroUsuariosAtenea existente = RegistroUsuariosAtenea.iniciar(procesoId, usuarioAeaId, LocalDate.now());
        when(registroUsuariosRepo.findByProcesoId(procesoId)).thenReturn(Optional.of(existente));
        when(registroUsuariosRepo.save(any(RegistroUsuariosAtenea.class))).thenAnswer(inv -> inv.getArgument(0));

        List<GuardarUsuarioPermisoAteneaInputDto> nuevos = List.of(
                new GuardarUsuarioPermisoAteneaInputDto(
                        UUID.randomUUID(), GrupoUsuarioAtenea.OTRAS_AREAS, "Laura", "Vega",
                        "27-31222333-8", "lvega@undec.edu.ar", "Dirección de Infraestructura",
                        "Directora de Infraestructura", TipoAccesoAtenea.CARGA, "Infraestructura e Inmuebles",
                        true, false, false
                )
        );

        RegistroUsuariosAteneaDto res = guardarService.guardar(procesoId, usuarioAeaId, nuevos);

        assertNotNull(res);
        assertEquals(1, res.usuarios().size());
        assertEquals("Vega", res.usuarios().get(0).apellido());
        assertEquals(GrupoUsuarioAtenea.OTRAS_AREAS, res.usuarios().get(0).grupoUsuario());
    }

    @Test
    @DisplayName("Guardar: Debe permitir actualizar matriz de permisos además de usuarios")
    void debeGuardarMatrizPermisosActualizada() {
        RegistroUsuariosAtenea existente = RegistroUsuariosAtenea.iniciar(procesoId, usuarioAeaId, LocalDate.now());
        when(registroUsuariosRepo.findByProcesoId(procesoId)).thenReturn(Optional.of(existente));
        when(registroUsuariosRepo.save(any(RegistroUsuariosAtenea.class))).thenAnswer(inv -> inv.getArgument(0));

        com.undec.acreditacion.application.input.GuardarItemMatrizPermisoInputDto itemDto =
                new com.undec.acreditacion.application.input.GuardarItemMatrizPermisoInputDto(
                        UUID.randomUUID(), "1. Infraestructura", "Todos", "Todos",
                        TipoPermisoMatrizAtenea.LECTURA, true, false, 1
                );

        RegistroUsuariosAteneaDto res = guardarService.guardar(procesoId, usuarioAeaId, null, List.of(itemDto));

        assertNotNull(res);
        assertEquals(1, res.matrizPermisos().size());
        assertFalse(res.matrizPermisos().get(0).permisoCa());
        assertTrue(res.matrizPermisos().get(0).permisoSgiAea());
    }

    @Test
    @DisplayName("Finalizar: Debe generar PDF con nomenclatura reglamentaria, subir a MinIO y completar actividad 3.3 en PT")
    void debeFinalizarRegistroCorrectamente() {
        RegistroUsuariosAtenea existente = RegistroUsuariosAtenea.iniciar(procesoId, usuarioAeaId, LocalDate.now());
        existente.agregarUsuario(UsuarioPermisoAtenea.crear(
                GrupoUsuarioAtenea.AEA, "Valeria", "Castro", "27-32111222-3",
                "vcastro@undec.edu.ar", "Área de Evaluación y Acreditación (AEA)",
                "Técnica AEA", TipoAccesoAtenea.ADMINISTRACION, "Formulario ATENEA", false, true, true
        ));
        when(registroUsuariosRepo.findByProcesoId(procesoId)).thenReturn(Optional.of(existente));

        ProcesoAcreditacion proceso = ProcesoAcreditacion.rehydrate(
                procesoId, carreraId, "PROC-001", "Acreditación Agronomía",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, EstadoProcesoAcreditacion.EN_AUTOEVALUACION,
                usuarioAeaId, Instant.now(), Instant.now()
        );
        when(procesoRepo.findById(procesoId)).thenReturn(Optional.of(proceso));

        Carrera carrera = Carrera.registrar(
                UUID.randomUUID(), "Ingeniería Agronómica", "AGRO", "Ciencias Agrarias",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero Agrónomo",
                5, "Carrera de grado", List.of()
        );
        when(carreraRepo.findById(carreraId)).thenReturn(Optional.of(carrera));

        RegistroConvocatoria convocatoria = RegistroConvocatoria.crear(
                procesoId, "RES-CONEAU-100", LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 12, 1), "RM 1254"
        );
        convocatoria.completarDatosConeau("RM 1254", FaseEvaluacion.PRIMERA_FASE, LocalDate.of(2026, 12, 1));
        when(convocatoriaRepo.findByProcesoId(procesoId)).thenReturn(Optional.of(convocatoria));

        when(storagePort.uploadFile(any(byte[].class), anyString(), eq(DocumentoAcreditacion.MIME_PDF)))
                .thenReturn("minio/01-Convocatoria y formalizacion CONEAU/RG-003.pdf");

        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-AGRO-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        ActividadPlanTrabajo act33 = new ActividadPlanTrabajo(
                UUID.randomUUID(), plan.getId(), 8, "3.3", "Definir usuarios y permisos de acceso",
                EstadoActividad.PENDIENTE, null, null, null, null, HojaPlanTrabajo.INFO,
                null, null, "Área de Evaluación y Acreditación (AEA)", PrioridadActividad.MEDIA,
                Instant.now(), Instant.now()
        );
        plan.agregarActividad(act33);
        when(planTrabajoRepo.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        when(registroUsuariosRepo.save(any(RegistroUsuariosAtenea.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroUsuariosAteneaDto res = finalizarService.finalizar(procesoId, usuarioAeaId);

        assertNotNull(res);
        assertTrue(res.finalizado());
        assertEquals(EstadoRegistroUsuariosAtenea.FINALIZADO, res.estado());
        assertNotNull(res.aprobadoEn());
        assertEquals("minio/01-Convocatoria y formalizacion CONEAU/RG-003.pdf", res.rutaDocumento());

        // Verificar que el archivo subido a MinIO tiene la nomenclatura oficial exigida
        verify(storagePort).uploadFile(
                any(byte[].class),
                eq("01-Convocatoria y formalizacion CONEAU/RG-003_Registro de usuarios de ATENEA_Ingenieria_Agronomica_1raFase_" + LocalDate.now().getYear() + ".pdf"),
                eq(DocumentoAcreditacion.MIME_PDF)
        );

        // Verificar que la actividad 3.3 en PlanTrabajo quedó COMPLETADA
        assertEquals(EstadoActividad.COMPLETADA, act33.getEstado());
        verify(planTrabajoRepo).saveActividad(act33);

        // Verificar registro de DocumentoAcreditacion formal
        verify(documentoRepo).save(any(DocumentoAcreditacion.class));
    }
}
