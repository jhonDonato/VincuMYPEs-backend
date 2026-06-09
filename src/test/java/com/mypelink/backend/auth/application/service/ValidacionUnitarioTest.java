package com.mypelink.backend.auth.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.auth.recovery.domain.model.PasswordReset;
import com.mypelink.backend.auth.recovery.domain.repository.PasswordResetRepository;
import com.mypelink.backend.shared.application.service.ConfiguracionService;
import com.mypelink.backend.usuarios.application.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidacionUnitarioTest {

    @Mock private PasswordResetRepository passwordResetRepository;
    @Mock private EmailService emailService;
    @Mock private ConfiguracionService configuracionService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "verificationEnabled", true);
        when(configuracionService.isModoMantenimiento()).thenReturn(false);
    }

    @Test
    void sendVerificationOtp_Success() {
        authService.sendVerificationOtp("user@upn.pe");

        verify(passwordResetRepository, times(1)).invalidatePreviousCodes("user@upn.pe");
        verify(passwordResetRepository, times(1)).save(any(PasswordReset.class));
        verify(emailService, times(1)).sendOtpEmail(eq("user@upn.pe"), anyString(), anyString());
    }

    @Test
    void verifyOtp_Success() {
        PasswordReset reset = new PasswordReset();
        reset.setUsed(false);

        when(passwordResetRepository.findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(
                eq("user@upn.pe"), eq("123456"), any())).thenReturn(Optional.of(reset));

        boolean result = authService.verifyOtp("user@upn.pe", "123456");

        assertTrue(result);
        assertTrue(reset.getUsed());
        verify(passwordResetRepository, times(1)).save(reset);
    }

    @Test
    void verifyOtp_InvalidCode() {
        when(passwordResetRepository.findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(
                eq("user@upn.pe"), eq("000000"), any())).thenReturn(Optional.empty());

        boolean result = authService.verifyOtp("user@upn.pe", "000000");

        assertFalse(result);
    }
}
