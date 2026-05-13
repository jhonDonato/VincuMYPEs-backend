package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.PostulacionResponse;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import com.mypelink.backend.usuarios.application.dto.EstudianteProfileResponse;
import com.mypelink.backend.usuarios.application.dto.UpdateEstudianteRequest;
import com.mypelink.backend.usuarios.application.service.EstudianteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final ProyectoService proyectoService;
    private final EstudianteService estudianteService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<EstudianteProfileResponse> obtenerPerfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(estudianteService.getProfile(userDetails.getUsername()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<EstudianteProfileResponse> actualizarPerfil(
            @Valid @RequestBody UpdateEstudianteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(estudianteService.updateProfile(userDetails.getUsername(), request));
    }

    @GetMapping("/me/postulaciones")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<PostulacionResponse>> misPostulaciones(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.misPostulaciones(userDetails.getUsername()));
    }
}