package com.mypelink.backend.ejecucion.infrastructure.rest;

import com.mypelink.backend.ejecucion.application.service.EntregableService;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/entregables")
@RequiredArgsConstructor
public class EntregableLockController {

    private final EntregableService entregableService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/{entregableId}/lock")
    @PreAuthorize("hasAnyAuthority('ESTUDIANTE', 'ROLE_ESTUDIANTE')")
    public ResponseEntity<Void> lock(@PathVariable Long proyectoId,
                                     @PathVariable Long entregableId,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        entregableService.adquirirBloqueo(entregableId, usuario);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{entregableId}/unlock")
    @PreAuthorize("hasAnyAuthority('ESTUDIANTE', 'ROLE_ESTUDIANTE')")
    public ResponseEntity<Void> unlock(@PathVariable Long proyectoId,
                                       @PathVariable Long entregableId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        entregableService.liberarBloqueo(entregableId, usuario);
        return ResponseEntity.ok().build();
    }
}