package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.AdminUsuarioResponse;
import com.mypelink.backend.usuarios.application.service.AdminUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @GetMapping
    public ResponseEntity<Page<AdminUsuarioResponse>> listarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String rol) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(adminUsuarioService.listarUsuarios(pageable, rol));
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
