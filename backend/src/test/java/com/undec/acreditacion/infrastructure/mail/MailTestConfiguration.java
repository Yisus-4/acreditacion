package com.undec.acreditacion.infrastructure.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;

@TestConfiguration
@Profile("test")
public class MailTestConfiguration {

    @Bean
    @Primary
    public JavaMailSender mockJavaMailSender() {
        JavaMailSender mockSender = Mockito.mock(JavaMailSender.class);
        Mockito.when(mockSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));
        return mockSender;
    }
}
