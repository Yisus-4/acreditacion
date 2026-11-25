package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConvocatoriaEscuelaDetalleDto;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.ports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarConvocatoriaEscuelaServiceTest {

    @Mock private ConvocatoriaEscuelaRepositoryPort convocatoriaRepository;
    @Mock private UnidadAcademicaRepositoryPort unidadAcademicaRepository;
    @Mock private CarreraRepositoryPort carreraRepository;
    @Mock private ProcesoAcreditacionRepositoryPort procesoRepository;
    @Mock private PlanTrabajoRepositoryPort planTrabajoRepository;
    @Mock private RegistroConformacionCaRepositoryPort registroCaRepository;
    @Mock private RegistroUsuariosAteneaRepositoryPort registroUsuariosAteneaRepository;

    private ConsultarConvocatoriaEscuelaService service;

    @BeforeEach
    void setUp() {
        service = new ConsultarConvocatoriaEscuelaService(
                convocatoriaRepository,
                unidadAcademicaRepository,
                carreraRepository,
                procesoRepository,
                planTrabajoRepository,
                registroCaRepository,
                registroUsuariosAteneaRepository
        );
    }

    @Test
    @DisplayName("Convocatoria recién confirmada con CA autocreada vacía (SOLICITADA_A_DC) debe quedar en INICIADO (Etapa 1 sin iniciar)")
    void convocatoriaRecienIniciadaNoSaltaAEscena3() {
        UUID convId = UUID.randomUUID();
        UUID uaId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ConvocatoriaCarrera item = ConvocatoriaCarrera.rehydrate(
                UUID.randomUUID(), convId, carreraId, true, true, true, procesoId, Instant.now(), Instant.now());
        Convocatoria conv = Convocatoria.rehydrate(
                convId, uaId, "CONV-2026-01", "Convocatoria 2026",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, "RES-01",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 1),
                EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA, null,
                userId, userId, Instant.now(), Instant.now(), Instant.now(),
                List.of(item));

        ProcesoAcreditacion proceso = ProcesoAcreditacion.iniciar(
                carreraId, "CONV-2026-AGRO-01", "Acreditación Agronomía",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, userId);
        PlanTrabajo planSinAvance = PlanTrabajo.inicializar(
                procesoId, "PT-001-AGRO-2026", TipoPlanTrabajo.PT_001_ACREDITACION);

        RegistroConformacionCa caAutoCreadaVacia = new RegistroConformacionCa(null, procesoId);
        caAutoCreadaVacia.solicitar(userId, 15);

        when(convocatoriaRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.empty());
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.empty());
        when(registroUsuariosAteneaRepository.findByProcesoId(procesoId)).thenReturn(Optional.empty());
        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(caAutoCreadaVacia));
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(planSinAvance));
        when(procesoRepository.findById(procesoId)).thenReturn(Optional.of(proceso));

        ConvocatoriaEscuelaDetalleDto dto = service.obtenerPorId(convId);

        assertEquals(1, dto.carreras().size());
        assertEquals("INICIADO", dto.carreras().get(0).estadoProceso());
    }

    @Test
    @DisplayName("Cuando el Plan de Trabajo fue enviado pasa a EN_AUTOEVALUACION (Etapa 3)")
    void planEnviadoAvanzaAEtapa3() {
        UUID convId = UUID.randomUUID();
        UUID uaId = UUID.randomUUID();
        UUID carreraId = UUID.randomUUID();
        UUID procesoId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ConvocatoriaCarrera item = ConvocatoriaCarrera.rehydrate(
                UUID.randomUUID(), convId, carreraId, true, true, true, procesoId, Instant.now(), Instant.now());
        Convocatoria conv = Convocatoria.rehydrate(
                convId, uaId, "CONV-2026-02", "Convocatoria 2026",
                TipoProcesoAcreditacion.EN_FUNCIONAMIENTO, "RES-02",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 1),
                EstadoConvocatoria.CONFIRMADA_DIRECTOR_ESCUELA, null,
                userId, userId, Instant.now(), Instant.now(), Instant.now(),
                List.of(item));

        PlanTrabajo planEnviado = PlanTrabajo.inicializar(
                procesoId, "PT-001-AGRO-2026", TipoPlanTrabajo.PT_001_ACREDITACION);
        planEnviado.setEstadoEnvio("ENVIADO");

        when(convocatoriaRepository.findById(convId)).thenReturn(Optional.of(conv));
        when(unidadAcademicaRepository.findById(uaId)).thenReturn(Optional.empty());
        when(carreraRepository.findById(carreraId)).thenReturn(Optional.empty());
        when(registroUsuariosAteneaRepository.findByProcesoId(procesoId)).thenReturn(Optional.empty());
        when(registroCaRepository.findByProcesoId(procesoId)).thenReturn(Optional.empty());
        when(planTrabajoRepository.findByProcesoId(procesoId)).thenReturn(Optional.of(planEnviado));

        ConvocatoriaEscuelaDetalleDto dto = service.obtenerPorId(convId);

        assertEquals("EN_AUTOEVALUACION", dto.carreras().get(0).estadoProceso());
    }
}
