package com.mypelink.backend.ejecucion.infrastructure.rest;

import com.mypelink.backend.ejecucion.application.dto.EntregableResponse;
import com.mypelink.backend.ejecucion.application.service.EntregableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mi-actividad")
@RequiredArgsConstructor
public class MiActividadController {

    private final EntregableService entregableService;

    // Devuelve TODOS los entregables del estudiante (con fechaEntrega) para la gráfica
    @GetMapping("/entregables")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE','ESTUDIANTE')")
    public ResponseEntity<List<EntregableResponse>> miActividad(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(entregableService.actividadDelEstudiante(userDetails.getUsername()));
    }
}