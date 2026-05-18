package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.ProyectoAdminResponse;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/proyectos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminProyectoController {

    private final ProyectoService proyectoService;

    /**
     * 1. GET /api/admin/proyectos
     * Alimenta la tabla central del AdminProyectosPage en React.
     */
    @GetMapping
    public ResponseEntity<List<ProyectoAdminResponse>> listarProyectosParaAdmin(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.listarParaAdmin(userDetails.getUsername()));
    }

    /**
     * 2. PATCH /api/admin/proyectos/{id}/ceder-gestion
     * Botón: "ArrowRightLeft" (Intercambio) - Permite a la MYPE evaluar a sus propios postulantes.
     */
    @PatchMapping("/{id}/ceder-gestion")
    public ResponseEntity<Void> cederGestion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        proyectoService.cederGestion(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * 3. POST /api/admin/proyectos/{id}/postulaciones/{postulacionId}/auditar-abandono
     * Botón: "AlertTriangle" (Auditar/Liberar) - Expulsa al alumno y reabre la oferta.
     */
    @PostMapping("/{id}/postulaciones/{postulacionId}/auditar-abandono")
    public ResponseEntity<Void> auditarAbandono(
            @PathVariable Long id,
            @PathVariable Long postulacionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        proyectoService.auditarAbandono(id, postulacionId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}