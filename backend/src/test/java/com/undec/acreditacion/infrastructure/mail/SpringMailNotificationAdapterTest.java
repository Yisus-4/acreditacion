package com.undec.acreditacion.infrastructure.mail;

import com.undec.acreditacion.domain.entities.NotificacionExpediente;
import com.undec.acreditacion.domain.entities.TipoEventoNotificacion;
import com.undec.acreditacion.domain.ports.NotificacionExpedienteRepositoryPort;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SpringMailNotificationAdapterTest {

    private JavaMailSender mailSender;
    private NotificacionExpedienteRepositoryPort notificacionRepository;
    private NormativaMailTemplates templates;
    private SpringMailNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        notificacionRepository = mock(NotificacionExpedienteRepositoryPort.class);
        templates = new NormativaMailTemplates();
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));

        adapter = new SpringMailNotificationAdapter(
            mailSender,
            notificacionRepository,
            templates,
            "acreditacion@undec.edu.ar"
        );
    }

    @Test
    @DisplayName("Despacho exitoso de correo registra auditoría con enviadoExitosamente = true")
    void testDespachoExitoso() {
        UUID procesoId = UUID.randomUUID();
        adapter.enviarSolicitudNormativa(
            procesoId,
            "director@undec.edu.ar",
            "Director de Carrera",
            "Ingeniería en Sistemas",
            "RES-2026-01",
            LocalDate.now().plusDays(7),
            "Adjuntar normativa"
        );

        verify(mailSender).send(any(MimeMessage.class));
        ArgumentCaptor<NotificacionExpediente> captor = ArgumentCaptor.forClass(NotificacionExpediente.class);
        verify(notificacionRepository).save(captor.capture());

        NotificacionExpediente notif = captor.getValue();
        assertEquals(procesoId, notif.getProcesoId());
        assertEquals(TipoEventoNotificacion.SOLICITUD_NORMATIVA_DC, notif.getTipoEvento());
        assertEquals("director@undec.edu.ar", notif.getDestinatarioEmail());
        assertTrue(notif.isEnviadoExitosamente());
        assertNull(notif.getErrorDetalle());
    }

    @Test
    @DisplayName("Fallo en SMTP captura error y registra auditoría con enviadoExitosamente = false")
    void testFalloSmtp_registraFalloSinPropagarExcepcion() {
        UUID procesoId = UUID.randomUUID();
        doThrow(new MailSendException("Error conectando a servidor SMTP"))
            .when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> adapter.enviarSolicitudNormativa(
            procesoId,
            "director@undec.edu.ar",
            "Director de Carrera",
            "Ingeniería en Sistemas",
            "RES-2026-01",
            LocalDate.now().plusDays(7),
            "Adjuntar normativa"
        ));

        ArgumentCaptor<NotificacionExpediente> captor = ArgumentCaptor.forClass(NotificacionExpediente.class);
        verify(notificacionRepository).save(captor.capture());

        NotificacionExpediente notif = captor.getValue();
        assertFalse(notif.isEnviadoExitosamente());
        assertNotNull(notif.getErrorDetalle());
        assertTrue(notif.getErrorDetalle().contains("SMTP"));
    }
}
