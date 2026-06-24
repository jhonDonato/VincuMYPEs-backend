package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.ActualizarMypeRequest;
import com.mypelink.backend.usuarios.application.dto.MypePerfilResponse;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/mypes")
@RequiredArgsConstructor
public class MypeController {

    private final ProyectoService proyectoService;

    // Perfil público de una MYPE — accesible por cualquier autenticado
    // El servicio determina qué datos mostrar según quién pregunta
    @GetMapping("/{id}/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MypePerfilResponse> obtenerPerfil(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                proyectoService.obtenerPerfilMype(id, userDetails.getUsername())
        );
    }

    // La MYPE edita su propio perfil
    @PutMapping("/{id}/perfil")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<MypePerfilResponse> actualizarPerfil(
            @PathVariable Long id,
            @RequestBody ActualizarMypeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                proyectoService.actualizarPerfil(id, request, userDetails.getUsername())
        );
    }

    // La MYPE obtiene su propio ID
    @GetMapping("/mi-perfil")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<MypePerfilResponse> miPerfil(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                proyectoService.miPerfilMype(userDetails.getUsername())
        );
    }
    @PostMapping("/upload-photo")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        String url = proyectoService.uploadFotoPerfil(file, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("url", url));
    }
}