package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.UsuarioResponse;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return usuarioRepository.findByEmailWithRole(userDetails.getUsername())
                .map(u -> ResponseEntity.ok(new UsuarioResponse(
                        u.getId(),
                        u.getNombre(),
                        u.getEmail(),
                        u.getTelefono(),
                        u.getFotoPerfil(),
                        u.getRol().getNombre()
                )))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}