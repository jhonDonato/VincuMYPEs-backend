package com.mypelink.backend.shared.infrastructure.rest;

import com.mypelink.backend.shared.application.dto.ModoMantenimientoRequest;
import com.mypelink.backend.shared.application.dto.ModoMantenimientoResponse;
import com.mypelink.backend.shared.application.service.ConfiguracionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/configuracion")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminConfiguracionController {

    private final ConfiguracionService configuracionService;

    @GetMapping
    public ResponseEntity<ModoMantenimientoResponse> obtenerModoMantenimiento() {
        return ResponseEntity.ok(configuracionService.obtenerModoMantenimiento());
    }

    @PutMapping("/mantenimiento")
    public ResponseEntity<ModoMantenimientoResponse> actualizarModoMantenimiento(
            @Valid @RequestBody ModoMantenimientoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(configuracionService.actualizarModoMantenimiento(request, userDetails.getUsername()));
    }
}