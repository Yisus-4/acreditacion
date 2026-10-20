package com.undec.acreditacion.infrastructure.mail;

import com.undec.acreditacion.domain.entities.CumplimientoPlazo;
import com.undec.acreditacion.domain.entities.NotificacionExpediente;
import com.undec.acreditacion.domain.entities.TipoEventoNotificacion;
import com.undec.acreditacion.domain.ports.EmailNotificationPort;
import com.undec.acreditacion.domain.ports.NotificacionExpedienteRepositoryPort;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class SpringMailNotificationAdapter implements EmailNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SpringMailNotificationAdapter.class);

    private final JavaMailSender mailSender;
    private final NotificacionExpedienteRepositoryPort notificacionRepository;
    private final NormativaMailTemplates mailTemplates;
    private final String fromEmail;

    public SpringMailNotificationAdapter(
        JavaMailSender mailSender,
        NotificacionExpedienteRepositoryPort notificacionRepository,
        NormativaMailTemplates mailTemplates,
        @Value("${spring.mail.username:acreditacion@undec.edu.ar}") String fromEmail
    ) {
        this.mailSender = Objects.requireNonNull(mailSender);
        this.notificacionRepository = Objects.requireNonNull(notificacionRepository);
        this.mailTemplates = Objects.requireNonNull(mailTemplates);
        this.fromEmail = fromEmail != null ? fromEmail : "acreditacion@undec.edu.ar";
    }

    @Override
    @Async
    public void enviarSolicitudNormativa(
        UUID procesoId,
        String destinatarioEmail,
        String destinatarioNombre,
        String carreraNombre,
        String codigoConvocatoria,
        LocalDate fechaLimite,
        String observaciones
    ) {
        String asunto = "UNdeC - Solicitud de Normativa de Carrera: " + carreraNombre;
        String html = mailTemplates.buildSolicitudNormativaHtml(
            destinatarioNombre, carreraNombre, codigoConvocatoria, fechaLimite, observaciones
        );
        despacharCorreo(procesoId, TipoEventoNotificacion.SOLICITUD_NORMATIVA_DC, destinatarioEmail, destinatarioNombre, asunto, html);
    }

    @Override
    @Async
    public void enviarRemisionRevision(
        UUID procesoId,
        String destinatarioEmail,
        String destinatarioNombre,
        String carreraNombre,
        String codigoConvocatoria,
        CumplimientoPlazo cumplimiento,
        int cantidadDocumentos
    ) {
        String asunto = "UNdeC - Remisión a Revisión de Normativa: " + carreraNombre;
        String html = mailTemplates.buildRemisionRevisionHtml(
            destinatarioNombre, carreraNombre, codigoConvocatoria, cumplimiento, cantidadDocumentos
        );
        despacharCorreo(procesoId, TipoEventoNotificacion.ENVIO_DOCUMENTACION_AEA, destinatarioEmail, destinatarioNombre, asunto, html);
    }

    @Override
    @Async
    public void enviarObservaciones(
        UUID procesoId,
        String destinatarioEmail,
        String destinatarioNombre,
        String carreraNombre,
        String codigoConvocatoria,
        String observacionesRevision,
        List<String> documentosObservados
    ) {
        String asunto = "UNdeC - Observaciones Técnicas sobre Normativa: " + carreraNombre;
        String html = mailTemplates.buildObservacionesHtml(
            destinatarioNombre, carreraNombre, codigoConvocatoria, observacionesRevision, documentosObservados
        );
        despacharCorreo(procesoId, TipoEventoNotificacion.OBSERVACION_NORMATIVA_DC, destinatarioEmail, destinatarioNombre, asunto, html);
    }

    @Override
    @Async
    public void enviarAprobacion(
        UUID procesoId,
        String destinatarioEmail,
        String destinatarioNombre,
        String carreraNombre,
        String codigoConvocatoria,
        String observaciones
    ) {
        String asunto = "UNdeC - Aprobación de Normativa: " + carreraNombre;
        String html = mailTemplates.buildAprobacionHtml(
            destinatarioNombre, carreraNombre, codigoConvocatoria, observaciones
        );
        despacharCorreo(procesoId, TipoEventoNotificacion.APROBACION_NORMATIVA_DC, destinatarioEmail, destinatarioNombre, asunto, html);
    }

    private void despacharCorreo(
        UUID procesoId,
        TipoEventoNotificacion tipoEvento,
        String destinatarioEmail,
        String destinatarioNombre,
        String asunto,
        String htmlContent
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(destinatarioEmail);
            helper.setSubject(asunto);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            notificacionRepository.save(
                NotificacionExpediente.exitoso(procesoId, tipoEvento, destinatarioEmail, destinatarioNombre, asunto)
            );
            log.info("Notificación {} enviada con éxito a {}", tipoEvento, destinatarioEmail);
        } catch (Exception e) {
            log.warn("Fallo al enviar notificación {} a {}: {}", tipoEvento, destinatarioEmail, e.getMessage());
            try {
                notificacionRepository.save(
                    NotificacionExpediente.fallido(procesoId, tipoEvento, destinatarioEmail, destinatarioNombre, asunto, e.getMessage())
                );
            } catch (Exception dbEx) {
                log.error("Fallo al registrar auditoría de notificación fallida: {}", dbEx.getMessage(), dbEx);
            }
        }
    }
}
