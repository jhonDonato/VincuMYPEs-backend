package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.AdminUsuarioResponse;
import com.mypelink.backend.usuarios.application.service.AdminUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @GetMapping
    public ResponseEntity<List<AdminUsuarioResponse>> listarUsuarios() {
        return ResponseEntity.ok(adminUsuarioService.listarUsuarios());
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstadoUsuario(@PathVariable Long id) {
        adminUsuarioService.cambiarEstadoUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/bypass-limite")
    public ResponseEntity<Void> cambiarBypassLimite(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        
        Integer nuevoLimite = body.get("nuevoLimite");
        adminUsuarioService.cambiarBypassLimite(id, nuevoLimite);
        return ResponseEntity.noContent().build();
    }
}
