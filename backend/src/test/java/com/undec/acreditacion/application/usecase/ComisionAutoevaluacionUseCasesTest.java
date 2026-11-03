package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.MiembroCaInputDto;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.*;
import com.undec.acreditacion.application.input.CrearSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.output.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComisionAutoevaluacionUseCasesTest {

    @Mock
    private RegistroConformacionCaRepositoryPort registroCaRepository;
    @Mock
    private ProcesoAcreditacionRepositoryPort procesoRepository;
    @Mock
    private CarreraRepositoryPort carreraRepository;
    @Mock
    private RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository;
    @Mock
    private PlanTrabajoRepositoryPort planTrabajoRepository;
    @Mock
    private DocumentStoragePort storagePort;
    @Mock
    private DocumentoAcreditacionRepositoryPort documentoRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CrearSolicitudNotificacionUseCase crearSolicitudNotificacionUseCase;

    private SolicitarConformacionCaService solicitarService;
    private ConsultarRegistroCaService consultarService;
    private GuardarRegistroCaBorradorService guardarBorradorService;
    private RemitirRegistroCaService remitirService;
    private AprobarRegistroCaService aprobarService;
    private ObservarRegistroCaService observarService;

    private UUID procesoId;
    private UUID carreraId;
    private UUID usuarioAeaId;
    private UUID usuarioDcId;
    private ProcesoAcreditacion proceso;
    private Carrera carreraGrado;

    @BeforeEach
    void setUp() {
        procesoId = UUID.randomUUID();
        carreraId = UUID.randomUUID();
        usuarioAeaId = UUID.randomUUID();
        usuarioDcId = UUID.randomUUID();

        solicitarService = new SolicitarConformacionCaService(
                registroCaRepository, procesoRepository, carreraRepository,
                registroConvocatoriaRepository, planTrabajoRepository
        );
        consultarService = new ConsultarRegistroCaService(registroCaRepository, procesoRepository);
        guardarBorradorService = new GuardarRegistroCaBorradorService(
                registroCaRepository, procesoRepository, carreraRepository, userRepository, crearSolicitudNotificacionUseCase
        );
        remitirService = new RemitirRegistroCaService(
                registroCaRepository, procesoRepository, storagePort, carreraRepository, userRepository, crearSolicitudNotificacionUseCase
        );
        aprobarService = new AprobarRegistroCaService(
                registroCaRepository, procesoRepository, carreraRepository,
                planTrabajoRepository, storagePort, documentoRepository
        );
        observarService = new ObservarRegistroCaService(registroCaRepository, procesoRepository);

        proceso = ProcesoAcreditacion.iniciar(
                carreraId, "CONV-2026", "Acreditación Ing Sistemas",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, usuarioAeaId
        );

        carreraGrado = Carrera.registrar(
                UUID.randomUUID(), "Ingeniería en Sistemas", "ISI", "Tecnología",
                ModalidadCarrera.PRESENCIAL, EstadoCarrera.EN_FUNCIONAMIENTO, "Ingeniero en Sistemas",
                5, "Carrera de grado", List.of()
        );
    }

    @Test
    @DisplayName("Solicitar conformación valida existencia de carrera de grado")
    void solicitarValidaCarrera() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitarService.solicitar(procesoId, usuarioAeaId))
                .isInstanceOf(com.undec.acreditacion.domain.exceptions.ResourceNotFoundException.class)
                .hasMessageContaining("carrera");
    }

    @Test
    @DisplayName("Solicitar conformación falla si el Plan de Trabajo o RG-001 no están formalizados")
    void solicitarFallaSiPrecondicionNoCumplida() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carreraGrado));

        RegistroConvocatoria regConvBorrador = RegistroConvocatoria.crear(
                procesoId, "RESFC-2026-1", LocalDate.now(), LocalDate.now().plusMonths(3), null
        );
        when(registroConvocatoriaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(regConvBorrador));

        assertThatThrownBy(() -> solicitarService.solicitar(procesoId, usuarioAeaId))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("RG-001 debe estar formalmente enviado");
    }

    @Test
    @DisplayName("Solicitar conformación exitosa con precondición cumplida")
    void solicitarExitosa() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carreraGrado));

        RegistroConvocatoria regConvEnviado = RegistroConvocatoria.crear(
                procesoId, "RESFC-2026-1", LocalDate.now(), LocalDate.now().plusMonths(3), null
        );
        regConvEnviado.marcarComoEnviado();
        when(registroConvocatoriaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(regConvEnviado));

        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        plan.marcarComoEnviado();
        plan.finalizarActividadElaboracionPlan(usuarioAeaId);
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.empty());
        when(registroCaRepository.save(any(RegistroConformacionCa.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroCaDto res = solicitarService.solicitar(procesoId, usuarioAeaId);

        assertThat(res.estado()).isEqualTo(EstadoRegistroCa.SOLICITADA_A_DC);
        assertThat(res.diasRestantes()).isEqualTo(15L);
        assertThat(res.semaforoTexto()).contains("Faltan 15 días");
    }

    @Test
    @DisplayName("Guardar borrador persiste miembros y estado")
    void guardarBorrador() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        RegistroConformacionCa ca = new RegistroConformacionCa(null, procesoId);
        ca.solicitar(usuarioAeaId);
        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(ca));
        when(registroCaRepository.save(any(RegistroConformacionCa.class))).thenAnswer(inv -> inv.getArgument(0));

        MiembroCaInputDto m = new MiembroCaInputDto(
                null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                "123", RolClaustroCa.COORDINADOR, 1
        );

        RegistroCaDto res = guardarBorradorService.guardarBorrador(procesoId, true, List.of(m));

        assertThat(res.estado()).isEqualTo(EstadoRegistroCa.EN_CARGA_BORRADOR);
        assertThat(res.coincideComisionCurricular()).isTrue();
        assertThat(res.miembros()).hasSize(1);
    }

    @Test
    @DisplayName("Remitir sube resolución y actualiza a REMITIDA_A_AEA")
    void remitirConResolucion() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        RegistroConformacionCa ca = new RegistroConformacionCa(null, procesoId);
        ca.solicitar(usuarioAeaId);
        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(ca));
        when(storagePort.uploadFile(any(), anyString(), anyString())).thenReturn("path/resolucion_ca.pdf");
        when(registroCaRepository.save(any(RegistroConformacionCa.class))).thenAnswer(inv -> inv.getArgument(0));

        MiembroCaInputDto m = new MiembroCaInputDto(
                null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                "123", RolClaustroCa.COORDINADOR, 1
        );

        byte[] pdf = "dummy pdf".getBytes();
        RegistroCaDto res = remitirService.remitir(procesoId, usuarioDcId, true, List.of(m), pdf, "res.pdf");

        assertThat(res.estado()).isEqualTo(EstadoRegistroCa.REMITIDA_A_AEA);
        assertThat(res.resolucionStoragePath()).isEqualTo("path/resolucion_ca.pdf");
    }

    @Test
    @DisplayName("Aprobar por AEA impacta en PlanTrabajo completando actividad en hoja INFO y guardando RG-002 PDF")
    void aprobarPorAeaImpactaPlanTrabajo() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carreraGrado));

        RegistroConformacionCa ca = new RegistroConformacionCa(null, procesoId);
        ca.solicitar(usuarioAeaId);
        MiembroComisionAutoevaluacion m = new MiembroComisionAutoevaluacion(
                null, null, "Perez", "Juan", null, "12345678", "juan@undec.edu.ar",
                null, RolClaustroCa.COORDINADOR, 1
        );
        ca.remitir(usuarioDcId, true, List.of(m), "path/res.pdf", "res.pdf", 100L);
        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(ca));
        when(storagePort.uploadFile(any(), anyString(), anyString())).thenReturn("01-Convocatoria/RG-002.pdf");
        when(registroCaRepository.save(any(RegistroConformacionCa.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanTrabajo plan = PlanTrabajo.inicializar(procesoId, "PT-001-ISI-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        ActividadPlanTrabajo actCa = new ActividadPlanTrabajo(
                UUID.randomUUID(), plan.getId(), 6, "3.1", "Solicitar la conformación de la CA de la carrera",
                EstadoActividad.PENDIENTE, null, null, null, null, HojaPlanTrabajo.INFO,
                null, null, "AEA", PrioridadActividad.ALTA, java.time.Instant.now(), java.time.Instant.now()
        );
        plan.agregarActividad(actCa);
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(plan));

        RegistroCaDto res = aprobarService.aprobar(procesoId, usuarioAeaId);

        assertThat(res.estado()).isEqualTo(EstadoRegistroCa.APROBADA);
        assertThat(res.registroGeneradoStoragePath()).isEqualTo("01-Convocatoria/RG-002.pdf");
        assertThat(actCa.getEstado()).isEqualTo(EstadoActividad.COMPLETADA);
        verify(documentoRepository).save(any(DocumentoAcreditacion.class));
    }

    @Test
    @DisplayName("Guardar borrador con miembro nuevo pendiente de alta emite solicitud de notificación a AEA")
    void guardarBorradorConSolicitudNotificacionAltaMiembro() {
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.of(carreraGrado));
        RegistroConformacionCa ca = new RegistroConformacionCa(null, procesoId);
        ca.solicitar(usuarioAeaId);
        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(ca));
        when(registroCaRepository.save(any(RegistroConformacionCa.class))).thenAnswer(inv -> inv.getArgument(0));

        MiembroCaInputDto miembroNuevo = new MiembroCaInputDto(
                null, "Albagli", "Karen Janet", null, "Karen Janet Albagli",
                "35850007", "35850007", "27-35850007-8", "kalbagli91@gmail.com",
                "albaagrimensura@gmail.com", "Docente", null, RolClaustroCa.DOCENTE,
                true, 1
        );

        RegistroCaDto res = guardarBorradorService.guardarBorrador(procesoId, usuarioDcId, true, List.of(miembroNuevo));

        assertThat(res.miembros()).hasSize(1);
        assertThat(res.miembros().get(0).pendienteAltaSistema()).isTrue();
        verify(crearSolicitudNotificacionUseCase, times(1)).registrarSolicitudAltaMiembro(
                eq(proceso.getId()),
                eq(usuarioDcId),
                any(),
                eq(carreraGrado.getNombre()),
                any()
        );
    }
}
