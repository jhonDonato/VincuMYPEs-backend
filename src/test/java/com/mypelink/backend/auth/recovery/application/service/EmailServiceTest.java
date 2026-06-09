package com.mypelink.backend.auth.recovery.application.service;

import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private EmailService emailService;
    private Resend resendMock;
    private Emails emailsMock;

    @BeforeEach
    void setUp() {
        emailService = new EmailService("test-key");
        resendMock = mock(Resend.class);
        emailsMock = mock(Emails.class);
        ReflectionTestUtils.setField(emailService, "resend", resendMock);
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@resend.dev");
        when(resendMock.emails()).thenReturn(emailsMock);
    }

    @Test
    void sendOtpEmail_Success() throws Exception {
        when(emailsMock.send(any(CreateEmailOptions.class)))
                .thenReturn(new CreateEmailResponse("email-id"));

        emailService.sendOtpEmail("test@test.com", "123456", "Código de verificación");

        verify(emailsMock, times(1)).send(any(CreateEmailOptions.class));
    }

    @Test
    void enviarCorreoNotificacion_Success() throws Exception {
        when(emailsMock.send(any(CreateEmailOptions.class)))
                .thenReturn(new CreateEmailResponse("email-id"));

        emailService.enviarCorreoNotificacion("test@test.com", "Título", "Mensaje", "Juan");

        verify(emailsMock, times(1)).send(any(CreateEmailOptions.class));
    }

    @Test
    void enviarCertificado_Success() throws Exception {
        when(emailsMock.send(any(CreateEmailOptions.class)))
                .thenReturn(new CreateEmailResponse("email-id"));

        emailService.enviarCertificado("est@test.com", "Juan", "Proyecto Web", "MYPE SAC", "CERT-001", "https://s3.url/cert.pdf");

        verify(emailsMock, times(1)).send(any(CreateEmailOptions.class));
    }
}
