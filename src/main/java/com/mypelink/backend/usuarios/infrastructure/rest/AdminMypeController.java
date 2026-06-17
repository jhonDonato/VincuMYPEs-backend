package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.MypePendienteResponse;
import com.mypelink.backend.usuarios.application.service.AdminMypeService;
import com.mypelink.backend.usuarios.domain.model.EstadoMype;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/mypes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminMypeController {

    private final AdminMypeService adminMypeService;

    @GetMapping("/pendientes")
    public ResponseEntity<List<MypePendienteResponse>> listarPendientes(
            @RequestParam(defaultValue = "PENDIENTE") String estado) {
        EstadoMype estadoEnum = EstadoMype.valueOf(estado.toUpperCase());
        return ResponseEntity.ok(adminMypeService.listarPorEstado(estadoEnum));
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<Void> aprobarMype(@PathVariable Long id) {
        adminMypeService.aprobarMype(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazarMype(@PathVariable Long id) {
        adminMypeService.rechazarMype(id);
        return ResponseEntity.noContent().build();
    }
}
