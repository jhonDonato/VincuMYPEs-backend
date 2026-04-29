package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.application.service.AuthService;
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
}
