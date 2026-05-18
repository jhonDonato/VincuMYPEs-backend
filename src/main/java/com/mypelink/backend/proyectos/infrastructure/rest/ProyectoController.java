package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.mypelink.backend.proyectos.application.dto.EditarProyectoRequest;

import java.util.List;

@RestController
@RequestMapping("/api/proyectos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_MYPE')")
public class ProyectoController {

    private final ProyectoService proyectoService;

    @GetMapping
    public ResponseEntity<Page<ProyectoResponse>> listar(
            @PageableDefault(size = 10, sort = "fechaCreacion") Pageable pageable) {
        return ResponseEntity.ok(proyectoService.listarPublicos(pageable));
    }

    // ✨ CORRECCIÓN: Hemos borrado los 3 métodos de Admin de este archivo porque ya están en AdminProyectoController

    @GetMapping("/mis-proyectos")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<List<ProyectoResponse>> misProyectos(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.listarPorMype(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProyectoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(proyectoService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<ProyectoResponse> crear(
            @Valid @RequestBody CrearProyectoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proyectoService.crear(request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/postular")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<PostulacionResponse> postular(
            @PathVariable Long id,
            @Valid @RequestBody PostulacionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proyectoService.postular(id, request, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/publicar")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<ProyectoResponse> publicar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.publicar(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/postulaciones")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<PostulacionResponse>> listarPostulaciones(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.listarPostulaciones(id, userDetails.getUsername()));
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MYPE', 'MYPE', 'ADMIN')")
    public ResponseEntity<ProyectoResponse> cerrarProyecto(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProyectoResponse response = proyectoService.cerrarProyecto(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<ProyectoResponse> editar(
            @PathVariable Long id,
            @Valid @RequestBody EditarProyectoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.editar(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        proyectoService.eliminar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mis-postulaciones")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<List<PostulacionResponse>> misPostulaciones(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.misPostulaciones(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/postulaciones/{postulacionId}/estado")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE', 'MYPE', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PostulacionResponse> cambiarEstadoPostulacion(
            @PathVariable Long id,
            @PathVariable Long postulacionId,
            @Valid @RequestBody CambiarEstadoPostulacionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(proyectoService.cambiarEstadoPostulacion(
                id, postulacionId, request, userDetails.getUsername()));
    }
}