package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.application.service.AuthService;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final MypeRepository mypeRepository;
    private final EstudianteRepository estudianteRepository;

    @Value("${app.email.verification.enabled:false}")
    private boolean verificationEnabled;

    @GetMapping("/check-telefono")
    public ResponseEntity<Boolean> existsByTelefono(@RequestParam String telefono) {
        return ResponseEntity.ok(usuarioRepository.existsByTelefono(telefono));
    }

    @PostMapping("/register/estudiante")
    public ResponseEntity<AuthResponse> registerEstudiante(@Valid @RequestBody RegisterEstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEstudiante(request));
    }

    @PostMapping("/register/mype")
    public ResponseEntity<Map<String, String>> registerMype(@Valid @RequestBody RegisterMypeRequest request) {
        authService.registerMype(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Registro exitoso. Su cuenta está pendiente de aprobación."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refreshAccessToken(body.get("refreshToken")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> body) {
        authService.logout(body.get("refreshToken"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    // ── ENDPOINTS DE VALIDACIÓN ──

    @GetMapping("/check-dni")
    public ResponseEntity<Boolean> existsByDni(@RequestParam String dni) {
        return ResponseEntity.ok(usuarioRepository.existsByDni(dni));
    }

    @GetMapping("/check-ruc")
    public ResponseEntity<Boolean> existsByRuc(@RequestParam String ruc) {
        return ResponseEntity.ok(mypeRepository.existsByRuc(ruc));
    }

    @GetMapping("/check-email")
    public ResponseEntity<Boolean> existsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(usuarioRepository.existsByEmail(email));
    }

    @GetMapping("/check-codigo")
    public ResponseEntity<Boolean> existsByCodigo(@RequestParam String codigo) {
        return ResponseEntity.ok(estudianteRepository.existsByCodigoEstudiante(codigo));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> sendVerificationOtp(@RequestParam String email) {
        if (!verificationEnabled) {
            return ResponseEntity.ok(Map.of("message", "Verificación desactivada"));
        }
        authService.sendVerificationOtp(email);
        return ResponseEntity.ok(Map.of("message", "Código enviado"));
    }

    @PostMapping("/confirm-email")
    public ResponseEntity<?> confirmVerificationOtp(@RequestParam String email, @RequestParam String otp) {
        if (!verificationEnabled) {
            return ResponseEntity.ok(Map.of("valid", true));
        }
        boolean valid = authService.verifyOtp(email, otp);
        return ResponseEntity.ok(Map.of("valid", valid));
    }
}
