package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.AbrirVacantesRequest;
import com.mypelink.backend.proyectos.application.dto.DecidirRequest;
import com.mypelink.backend.proyectos.application.dto.ProyectoAdminResponse;
import com.mypelink.backend.proyectos.application.service.AdminDecisionService;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final AdminDecisionService adminDecisionService;

    @GetMapping
    public ResponseEntity<Page<ProyectoAdminResponse>> listarProyectosParaAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @AuthenticationPrincipal UserDetails userDetails) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(proyectoService.listarParaAdmin(pageable));
    }

    @PatchMapping("/{id}/ceder-gestion")
    public ResponseEntity<Void> cederGestion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        proyectoService.cederGestion(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/postulaciones/{postulacionId}/auditar-abandono")
    public ResponseEntity<Void> auditarAbandono(
            @PathVariable Long id,
            @PathVariable Long postulacionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        proyectoService.auditarAbandono(id, postulacionId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarProyecto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        proyectoService.cancelarProyecto(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/abrir-vacantes")
    public ResponseEntity<Void> abrirVacantes(
            @PathVariable Long id,
            @RequestBody AbrirVacantesRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        proyectoService.abrirVacantes(id, request.estudianteIds(), userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{id}/decidir")
    public ResponseEntity<Void> decidir(
            @PathVariable Long id,
            @Valid @RequestBody DecidirRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        adminDecisionService.decidir(id, request, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}