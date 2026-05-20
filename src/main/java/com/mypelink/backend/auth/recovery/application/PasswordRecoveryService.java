package com.mypelink.backend.auth.recovery.application;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.auth.recovery.domain.model.PasswordReset;
import com.mypelink.backend.auth.recovery.domain.repository.PasswordResetRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void requestOtp(String email) {
        // No revelar si el email existe o no (seguridad)
        if (!usuarioRepository.existsByEmail(email)) {
            log.info("Solicitud OTP para email no registrado: {}", email);
            return;
        }

        // Invalidar códigos anteriores
        passwordResetRepository.invalidatePreviousCodes(email);

        // Generar OTP de 6 dígitos
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));

        // Guardar en BD
        PasswordReset reset = PasswordReset.builder()
                .email(email)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        passwordResetRepository.save(reset);

        // Enviar email
        emailService.sendOtpEmail(email, otp);
        log.info("OTP generado y enviado para: {}", email);
    }

    @Transactional
    public String verifyOtp(String email, String otp) {
        PasswordReset reset = passwordResetRepository
                .findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(email, otp, LocalDateTime.now())
                .orElseThrow(() -> {
                    log.warn("OTP inválido o expirado para: {}", email);
                    return new BusinessException("Código inválido o expirado");
                });

        // Marcar como usado
        reset.setUsed(true);
        passwordResetRepository.save(reset);

        // Generar token temporal (5 min) para cambiar contraseña
        UserDetails userDetails = User.builder()
                .username(email)
                .password("")
                .authorities("ROLE_RESET_PASSWORD")
                .build();

        Map<String, Object> claims = Map.of("purpose", "password-reset");

        log.info("OTP verificado exitosamente para: {}", email);
        return jwtService.generateToken(userDetails, claims);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Validar token
        String email;
        try {
            email = jwtService.extractUsername(token);
        } catch (Exception e) {
            throw new BusinessException("Token inválido o expirado");
        }

        // Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado para reset: {}", email);
                    return new BusinessException("Usuario no encontrado");
                });

        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);

        log.info("Contraseña actualizada exitosamente para: {}", email);
    }

    // Limpieza programada de códigos expirados
    @Transactional
    public void cleanExpiredCodes() {
        passwordResetRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusHours(24));
        log.debug("Códigos expirados eliminados");
    }
}