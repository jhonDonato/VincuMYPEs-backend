package com.mypelink.backend.auth.recovery.application;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.auth.recovery.domain.model.PasswordReset;
import com.mypelink.backend.auth.recovery.domain.repository.PasswordResetRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordResetRepository passwordResetRepository;
    @Mock private EmailService emailService;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordRecoveryService passwordRecoveryService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().id(1L).nombre("Juan").email("juan@test.com")
                .password("encoded")
                .rol(Role.builder().nombre("ROLE_ESTUDIANTE").build())
                .build();
    }

    @Test
    void requestOtp_Success() {
        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(true);
        when(passwordResetRepository.save(any(PasswordReset.class))).thenReturn(null);

        passwordRecoveryService.requestOtp("juan@test.com");

        verify(emailService, times(1)).sendOtpEmail(eq("juan@test.com"), anyString(), anyString());
    }

    @Test
    void requestOtp_ShouldFail_WhenEmailNotRegistered() {
        when(usuarioRepository.existsByEmail("unknown@test.com")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> passwordRecoveryService.requestOtp("unknown@test.com"));
    }

    @Test
    void verifyOtp_Success() {
        PasswordReset reset = PasswordReset.builder()
                .email("juan@test.com").otpCode("123456").used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10)).build();
        when(passwordResetRepository.findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(
                eq("juan@test.com"), eq("123456"), any())).thenReturn(Optional.of(reset));
        when(jwtService.generateToken(any(), anyMap())).thenReturn("reset-token");

        String token = passwordRecoveryService.verifyOtp("juan@test.com", "123456");

        assertEquals("reset-token", token);
        assertTrue(reset.getUsed());
    }

    @Test
    void verifyOtp_ShouldFail_WhenInvalid() {
        when(passwordResetRepository.findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(
                eq("juan@test.com"), eq("wrong"), any())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> passwordRecoveryService.verifyOtp("juan@test.com", "wrong"));
    }

    @Test
    void resetPassword_Success() {
        when(jwtService.extractUsername("valid-token")).thenReturn("juan@test.com");
        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("newEncoded");

        passwordRecoveryService.resetPassword("valid-token", "NewPass1!");

        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void resetPassword_ShouldFail_WhenTokenInvalid() {
        when(jwtService.extractUsername("bad-token")).thenThrow(new RuntimeException("Invalid"));

        assertThrows(BusinessException.class,
                () -> passwordRecoveryService.resetPassword("bad-token", "NewPass1!"));
    }

    @Test
    void cleanExpiredCodes_Success() {
        passwordRecoveryService.cleanExpiredCodes();
        verify(passwordResetRepository, times(1)).deleteByExpiresAtBefore(any());
    }
}
