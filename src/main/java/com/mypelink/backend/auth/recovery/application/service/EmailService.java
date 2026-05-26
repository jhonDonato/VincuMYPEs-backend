package com.mypelink.backend.auth.recovery.application.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    public EmailService(@Value("${resend.api-key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public void sendOtpEmail(String to, String otpCode, String subject) {
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from("Linkuy <" + fromEmail + ">")
                    .to(to)
                    .subject(subject)
                    .html(buildEmailTemplate(otpCode, subject))
                    .build();

            resend.emails().send(options);
            log.info("OTP enviado exitosamente a: {}", to);
        } catch (ResendException e) {
            log.error("Error al enviar email OTP a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error al enviar el código de verificación", e);
        }
    }

    public void enviarCorreoNotificacion(String to, String titulo, String mensaje, String nombreUsuario) {
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from("Linkuy <" + fromEmail + ">")
                    .to(to)
                    .subject("🔔 " + titulo)
                    .html(buildNotificacionTemplate(titulo, mensaje, nombreUsuario))
                    .build();

            resend.emails().send(options);
            log.info("Correo de notificación enviado a: {}", to);
        } catch (ResendException e) {
            log.error("Error al enviar correo de notificación a {}: {}", to, e.getMessage());
        }
    }

    private String buildEmailTemplate(String otpCode, String subject) {
        return """
            <div style="font-family: Arial, Helvetica, sans-serif; max-width: 480px; margin: 0 auto;">
                <div style="background: linear-gradient(135deg, #1B6FE8, #0E54C4); padding: 32px; border-radius: 8px 8px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 700;">Linkuy</h1>
                </div>
                <div style="background: white; padding: 32px; border: 1px solid #E5E7EB; border-top: none; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #0F1F3D; margin: 0 0 16px; font-size: 20px;">%s</h2>
                    <p style="color: #6B7280; margin: 0 0 24px; font-size: 14px; line-height: 1.6;">
                        Usa el siguiente código de verificación:
                    </p>
                    <div style="background: #F3F4F6; padding: 20px; border-radius: 8px; text-align: center; margin-bottom: 24px;">
                        <span style="font-size: 36px; font-weight: 700; letter-spacing: 12px; color: #0F1F3D; font-family: 'Courier New', monospace;">
                            %s
                        </span>
                    </div>
                    <p style="color: #9CA3AF; margin: 0 0 8px; font-size: 12px;">
                        Este código expira en <strong>10 minutos</strong>.
                    </p>
                    <p style="color: #9CA3AF; margin: 0; font-size: 12px;">
                        Si no solicitaste este código, ignora este mensaje.
                    </p>
                </div>
            </div>
            """.formatted(subject, otpCode);
    }

    private String buildNotificacionTemplate(String titulo, String mensaje, String nombreUsuario) {
        return """
        <div style="font-family: Arial, Helvetica, sans-serif; max-width: 480px; margin: 0 auto;">
            <div style="background: linear-gradient(135deg, #1B6FE8, #0E54C4); padding: 32px; border-radius: 8px 8px 0 0;">
                <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 700;">Linkuy</h1>
            </div>
            <div style="background: white; padding: 32px; border: 1px solid #E5E7EB; border-top: none; border-radius: 0 0 8px 8px;">
                <h2 style="color: #0F1F3D; margin: 0 0 8px; font-size: 20px;">¡Hola, %s!</h2>
                <p style="color: #6B7280; margin: 0 0 16px; font-size: 14px;">
                    Tienes una nueva notificación en Linkuy:
                </p>
                <div style="background: #F0F6FF; border-left: 4px solid #1B6FE8; padding: 16px; border-radius: 4px; margin-bottom: 24px;">
                    <h3 style="color: #0F1F3D; margin: 0 0 8px; font-size: 16px;">%s</h3>
                    <p style="color: #6B7280; margin: 0; font-size: 14px; line-height: 1.6;">%s</p>
                </div>
                <a href="http://localhost:5173/mis-postulaciones" style="display: inline-block; background: #1B6FE8; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px;">
                    Ver en Linkuy
                </a>
                <p style="color: #9CA3AF; margin: 16px 0 0; font-size: 12px;">
                    Linkuy - Plataforma de vinculación UPN
                </p>
            </div>
        </div>
        """.formatted(nombreUsuario, titulo, mensaje);
    }
}