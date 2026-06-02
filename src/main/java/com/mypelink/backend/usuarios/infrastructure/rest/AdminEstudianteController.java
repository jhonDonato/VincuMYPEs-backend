package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.application.dto.ActualizarLimiteProyectosRequest;
import com.mypelink.backend.usuarios.application.dto.EstudianteAdminResponse;
import com.mypelink.backend.usuarios.application.service.AdminEstudianteService;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/estudiantes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminEstudianteController {

    private final AdminEstudianteService adminEstudianteService;

    @GetMapping
    public ResponseEntity<List<EstudianteAdminResponse>> listar(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(adminEstudianteService.listarTodos(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/limite-proyectos")
    public ResponseEntity<EstudianteAdminResponse> actualizarLimite(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarLimiteProyectosRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(adminEstudianteService.actualizarLimiteProyectos(
                id, request, userDetails.getUsername()
        ));
    }

    @PatchMapping("/usuario/{usuarioId}/limite-proyectos")
    public ResponseEntity<EstudianteAdminResponse> actualizarLimitePorUsuarioId(
            @PathVariable Long usuarioId,
            @Valid @RequestBody ActualizarLimiteProyectosRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(adminEstudianteService.actualizarLimitePorUsuarioId(
                usuarioId, request, userDetails.getUsername()
        ));
    }
}