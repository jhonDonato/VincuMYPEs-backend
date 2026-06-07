// com.mypelink.backend.calificaciones.infrastructure.rest.CalificacionController
package com.mypelink.backend.calificaciones.infrastructure.rest;

import com.mypelink.backend.calificaciones.application.dto.*;
import com.mypelink.backend.calificaciones.application.service.CalificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calificaciones")
@RequiredArgsConstructor
public class CalificacionController {

    private final CalificacionService calificacionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('MYPE', 'ESTUDIANTE', 'ROLE_MYPE', 'ROLE_ESTUDIANTE')")
    public ResponseEntity<Void> crear(
            @Valid @RequestBody CrearCalificacionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        calificacionService.crear(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me/pendientes")
    @PreAuthorize("hasAnyAuthority('MYPE', 'ESTUDIANTE', 'ROLE_MYPE', 'ROLE_ESTUDIANTE')")
    public ResponseEntity<List<CalificacionPendienteResponse>> pendientes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(calificacionService.obtenerPendientes(userDetails.getUsername()));
    }

    @GetMapping("/usuarios/{id}/rating")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingResponse> obtenerRating(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(calificacionService.obtenerRating(id, userDetails.getUsername()));
    }
}