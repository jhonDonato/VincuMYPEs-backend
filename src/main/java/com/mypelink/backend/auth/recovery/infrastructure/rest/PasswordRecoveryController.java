package com.mypelink.backend.auth.recovery.infrastructure.rest;

import com.mypelink.backend.auth.recovery.application.PasswordRecoveryService;
import com.mypelink.backend.auth.recovery.application.dto.ForgotPasswordRequest;
import com.mypelink.backend.auth.recovery.application.dto.ResetPasswordRequest;
import com.mypelink.backend.auth.recovery.application.dto.VerifyOtpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordRecoveryController {

    private final PasswordRecoveryService recoveryService;

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        recoveryService.requestOtp(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Si el email está registrado, recibirás un código de verificación"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String token = recoveryService.verifyOtp(request.email(), request.otp());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        recoveryService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
    }
}