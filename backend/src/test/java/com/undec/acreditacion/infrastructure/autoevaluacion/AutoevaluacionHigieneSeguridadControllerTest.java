package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.ActualizarGuiaHigieneRequest;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.GuiaHigieneSeguridadJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataGuiaHigieneDocumentoRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataGuiaHigieneSeguridadRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.service.GuiaHigieneSeguridadClonerService;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataInstitucionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AutoevaluacionHigieneSeguridadControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SpringDataGuiaHigieneSeguridadRepository guiaRepository;

    @Mock
    private SpringDataGuiaHigieneDocumentoRepository documentoRepository;

    @Mock
    private SpringDataInstitucionRepository institucionRepository;

    @Mock
    private PlanTrabajoRepositoryPort planTrabajoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuiaHigieneSeguridadClonerService clonerService;

    private AutoevaluacionHigieneSeguridadController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AutoevaluacionHigieneSeguridadController(
                guiaRepository,
                documentoRepository,
                institucionRepository,
                planTrabajoRepository,
                userRepository,
                clonerService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AutoevaluacionExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateRole(String role) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user@undec.edu.ar",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("GET /maestro retorna 200 con la guía maestra institucional")
    void debeObtenerGuiaMaestra() throws Exception {
        GuiaHigieneSeguridadJpaEntity maestro = new GuiaHigieneSeguridadJpaEntity();
        maestro.setId(UUID.randomUUID());
        maestro.setEsMaestro(true);
        maestro.setEdicion("01");
        maestro.setElaboradoPor("Mara Rovero (AEA)");
        maestro.setInstanciaResponsableUa("Secretaría de Infraestructura");

        when(guiaRepository.findGuiaMaestraCompleta()).thenReturn(Optional.of(maestro));

        mockMvc.perform(get("/api/autoevaluacion/higiene-seguridad/maestro")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esMaestro", is(true)))
                .andExpect(jsonPath("$.edicion", is("01")));
    }

    @Test
    @DisplayName("GET /procesos/{procesoId} retorna guía existente si ya fue clonada")
    void debeObtenerGuiaExistentePorProceso() throws Exception {
        UUID procesoId = UUID.randomUUID();
        GuiaHigieneSeguridadJpaEntity guia = new GuiaHigieneSeguridadJpaEntity();
        guia.setId(UUID.randomUUID());
        guia.setProcesoId(procesoId);
        guia.setEsMaestro(false);
        guia.setEstado("PENDIENTE");

        when(guiaRepository.findByProcesoIdCompleta(procesoId)).thenReturn(Optional.of(guia));

        mockMvc.perform(get("/api/autoevaluacion/higiene-seguridad/procesos/{procesoId}", procesoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procesoId", is(procesoId.toString())))
                .andExpect(jsonPath("$.estado", is("PENDIENTE")));

        verify(clonerService, never()).clonarGuiaMaestraParaProceso(any());
    }

    @Test
    @DisplayName("CRIT-01: GET /procesos/{procesoId} clona de forma idempotente ante DataIntegrityViolationException por concurrencia")
    void debeManejarConcurrenciaAlClonar() throws Exception {
        UUID procesoId = UUID.randomUUID();

        // 1. Primer lookup retorna empty
        GuiaHigieneSeguridadJpaEntity reloaded = new GuiaHigieneSeguridadJpaEntity();
        reloaded.setId(UUID.randomUUID());
        reloaded.setProcesoId(procesoId);
        reloaded.setEsMaestro(false);
        reloaded.setEstado("PENDIENTE");

        when(guiaRepository.findByProcesoIdCompleta(procesoId))
                .thenReturn(Optional.empty()) // Primer intento
                .thenReturn(Optional.of(reloaded)); // Segundo intento tras captura de colisión

        // 2. El helper lanza DataIntegrityViolationException simulando carrera con otra hebra
        when(clonerService.clonarGuiaMaestraParaProceso(procesoId))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uq_guia_higiene_proceso_id"));

        mockMvc.perform(get("/api/autoevaluacion/higiene-seguridad/procesos/{procesoId}", procesoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procesoId", is(procesoId.toString())));

        verify(clonerService).clonarGuiaMaestraParaProceso(procesoId);
        verify(guiaRepository, times(2)).findByProcesoIdCompleta(procesoId);
    }

    @Test
    @DisplayName("PUT /{id} actualiza campos de la guía")
    void debeActualizarGuia() throws Exception {
        UUID id = UUID.randomUUID();
        GuiaHigieneSeguridadJpaEntity guia = new GuiaHigieneSeguridadJpaEntity();
        guia.setId(id);
        guia.setInstanciaResponsableUa("Anterior");

        when(guiaRepository.findByIdCompleta(id)).thenReturn(Optional.of(guia));
        when(guiaRepository.save(any(GuiaHigieneSeguridadJpaEntity.class))).thenAnswer(i -> i.getArgument(0));

        ActualizarGuiaHigieneRequest req = new ActualizarGuiaHigieneRequest(
                "Nueva Instancia Responsable",
                "Política Vigente",
                true,
                "Observaciones de prueba",
                null
        );

        mockMvc.perform(put("/api/autoevaluacion/higiene-seguridad/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanciaResponsableUa", is("Nueva Instancia Responsable")))
                .andExpect(jsonPath("$.aplicaUnidadesApoyo", is(true)));

        assertEquals("Nueva Instancia Responsable", guia.getInstanciaResponsableUa());
        assertEquals("Política Vigente", guia.getPoliticaUnidadesApoyo());
        assertEquals(true, guia.getAplicaUnidadesApoyo());
    }
}
