package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.application.service.AuthService;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepository;
    private final MypeRepository mypeRepository;
    private final EstudianteRepository estudianteRepository;

    @PostMapping("/register/estudiante")
    public ResponseEntity<AuthResponse> registerEstudiante(@Valid @RequestBody RegisterEstudianteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEstudiante(request));
    }

    @PostMapping("/register/mype")
    public ResponseEntity<AuthResponse> registerMype(@Valid @RequestBody RegisterMypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerMype(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
}