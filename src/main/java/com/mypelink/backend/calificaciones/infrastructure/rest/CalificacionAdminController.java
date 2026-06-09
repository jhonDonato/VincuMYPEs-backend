// com.mypelink.backend.calificaciones.infrastructure.rest.CalificacionAdminController.java
package com.mypelink.backend.calificaciones.infrastructure.rest;

import com.mypelink.backend.calificaciones.application.dto.CalificacionAdminResponse;
import com.mypelink.backend.calificaciones.application.dto.EditarCalificacionRequest;
import com.mypelink.backend.calificaciones.application.service.CalificacionAdminService;
import jakarta.servlet.http.HttpServletRequest;
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

@RestController
@RequestMapping("/api/admin/calificaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class CalificacionAdminController {

    private final CalificacionAdminService calificacionAdminService;

    @GetMapping
    public ResponseEntity<Page<CalificacionAdminResponse>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @AuthenticationPrincipal UserDetails userDetails) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(calificacionAdminService.listarTodasCalificaciones(pageable, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CalificacionAdminResponse> obtenerCalificacion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(calificacionAdminService.obtenerCalificacion(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CalificacionAdminResponse> editarCalificacion(
            @PathVariable Long id,
            @Valid @RequestBody EditarCalificacionRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(calificacionAdminService.editarCalificacion(
                id, request, userDetails.getUsername(), httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCalificacion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest) {
        calificacionAdminService.eliminarCalificacion(id, userDetails.getUsername(), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<CalificacionAdminResponse> buscarPorProyectoYUsuarios(
            @RequestParam Long proyectoId,
            @RequestParam Long calificadorId,
            @RequestParam Long calificadoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(calificacionAdminService.obtenerCalificacionPorProyectoYUsuarios(
                proyectoId, calificadorId, calificadoId, userDetails.getUsername()));
    }
}