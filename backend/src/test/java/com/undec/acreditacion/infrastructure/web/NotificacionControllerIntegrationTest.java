package com.undec.acreditacion.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.ConsultarNotificacionesUseCase;
import com.undec.acreditacion.application.input.GestionarSolicitudNotificacionUseCase;
import com.undec.acreditacion.application.output.SolicitudNotificacionDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.SolicitudNotificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificacionControllerIntegrationTest {

    private MockMvc mockMvc;
    private ConsultarNotificacionesUseCase consultarUseCase;
    private GestionarSolicitudNotificacionUseCase gestionarUseCase;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;

    private UUID notifId;

    @BeforeEach
    void setUp() {
        consultarUseCase = mock(ConsultarNotificacionesUseCase.class);
        gestionarUseCase = mock(GestionarSolicitudNotificacionUseCase.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper();

        notifId = UUID.randomUUID();

        NotificacionController controller = new NotificacionController(
                consultarUseCase,
                gestionarUseCase,
                new com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider(userRepository)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/notificaciones lista notificaciones")
    void listarNotificaciones() throws Exception {
        SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                notifId,
                SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO,
                "Solicitud de alta",
                "Mensaje de solicitud",
                "{}",
                UUID.randomUUID(),
                "Ingeniería en Sistemas",
                UUID.randomUUID(),
                "Director DC",
                "AEA",
                SolicitudNotificacion.ESTADO_PENDIENTE,
                false,
                null,
                null,
                Instant.now()
        );

        when(consultarUseCase.listar("AEA", null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/notificaciones?rol=AEA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notifId.toString()))
                .andExpect(jsonPath("$[0].tipo").value(SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO))
                .andExpect(jsonPath("$[0].estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("GET /api/notificaciones/conteo-pendientes retorna conteo")
    void conteoPendientes() throws Exception {
        when(consultarUseCase.contarPendientes("AEA")).thenReturn(3L);

        mockMvc.perform(get("/api/notificaciones/conteo-pendientes?rol=AEA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteo").value(3));
    }

    @Test
    @DisplayName("POST /api/notificaciones/{id}/aceptar acepta solicitud")
    void aceptarSolicitud() throws Exception {
        SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                notifId,
                SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO,
                "Solicitud de alta",
                "Mensaje de solicitud",
                "{}",
                UUID.randomUUID(),
                "Ingeniería en Sistemas",
                UUID.randomUUID(),
                "Director DC",
                "AEA",
                SolicitudNotificacion.ESTADO_ACEPTADA,
                true,
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );

        when(gestionarUseCase.aceptar(eq(notifId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/notificaciones/{id}/aceptar", notifId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notifId.toString()))
                .andExpect(jsonPath("$.estado").value("ACEPTADA"))
                .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    @DisplayName("POST /api/notificaciones/{id}/rechazar rechaza solicitud")
    void rechazarSolicitud() throws Exception {
        SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                notifId,
                SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO,
                "Solicitud de alta",
                "Mensaje de solicitud",
                "{}",
                UUID.randomUUID(),
                "Ingeniería en Sistemas",
                UUID.randomUUID(),
                "Director DC",
                "AEA",
                SolicitudNotificacion.ESTADO_RECHAZADA,
                true,
                UUID.randomUUID(),
                Instant.now(),
                Instant.now()
        );

        when(gestionarUseCase.rechazar(eq(notifId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/notificaciones/{id}/rechazar", notifId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notifId.toString()))
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
    }

    @Test
    @DisplayName("PATCH /api/notificaciones/{id}/marcar-leida marca leída")
    void marcarLeida() throws Exception {
        SolicitudNotificacionDto dto = new SolicitudNotificacionDto(
                notifId,
                SolicitudNotificacion.TIPO_ALTA_MIEMBRO_CATALOGO,
                "Solicitud de alta",
                "Mensaje de solicitud",
                "{}",
                UUID.randomUUID(),
                "Ingeniería en Sistemas",
                UUID.randomUUID(),
                "Director DC",
                "AEA",
                SolicitudNotificacion.ESTADO_PENDIENTE,
                true,
                null,
                null,
                Instant.now()
        );

        when(gestionarUseCase.marcarLeida(eq(notifId))).thenReturn(dto);

        mockMvc.perform(patch("/api/notificaciones/{id}/marcar-leida", notifId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
    }
}
