package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.application.service.AdminUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    // ==================== EXISTENTES ====================

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
            @RequestBody ActualizarLimiteProyectosRequest request) {
        adminUsuarioService.cambiarBypassLimite(id, request.nuevoLimite());
        return ResponseEntity.noContent().build();
    }

    // ==================== NUEVOS ENDPOINTS ====================

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDetailAdminResponse> obtenerDetalleUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(adminUsuarioService.obtenerDetalleUsuario(id));
    }

    @PostMapping
    public ResponseEntity<AdminUsuarioResponse> crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminUsuarioService.crearUsuario(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUsuarioResponse> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioRequest request) {
        return ResponseEntity.ok(adminUsuarioService.actualizarUsuario(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean permanente) {
        adminUsuarioService.eliminarUsuario(id, permanente);
        return ResponseEntity.noContent().build();
    }
}