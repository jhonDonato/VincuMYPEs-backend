package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.PostulacionResponse;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.application.service.UsuarioService;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return usuarioRepository.findByEmailWithRole(userDetails.getUsername())
                .map(u -> ResponseEntity.ok(new UsuarioResponse(
                        u.getId(), u.getNombre(), u.getEmail(),
                        u.getTelefono(), u.getFotoPerfil(), u.getRol().getNombre()
                )))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    // Actualizar nombre y teléfono
    @PatchMapping("/me/info")
    public ResponseEntity<UsuarioResponse> actualizarInfo(
            @RequestBody ActualizarInfoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(usuarioService.actualizarInfo(userDetails.getUsername(), request));
    }

    // Cambiar contraseña
    @PatchMapping("/me/password")
    public ResponseEntity<Void> cambiarPassword(
            @RequestBody CambiarPasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        usuarioService.cambiarPassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    // Cambiar email
    @PatchMapping("/me/email")
    public ResponseEntity<UsuarioResponse> cambiarEmail(
            @RequestBody CambiarEmailRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(usuarioService.cambiarEmail(userDetails.getUsername(), request));
    }

    // Desactivar cuenta
    @PatchMapping("/me/desactivar")
    public ResponseEntity<Void> desactivarCuenta(
            @RequestBody ConfirmarPasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        usuarioService.desactivarCuenta(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/postulaciones")
    @PreAuthorize("hasRole('ESTUDIANTE')")
    public ResponseEntity<List<PostulacionResponse>> misPostulaciones(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.misPostulaciones(userDetails.getUsername()));
    }
}