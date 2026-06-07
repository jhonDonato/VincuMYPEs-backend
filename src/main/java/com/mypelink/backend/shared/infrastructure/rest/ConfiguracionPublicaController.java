package com.mypelink.backend.shared.infrastructure.rest;

import com.mypelink.backend.shared.application.dto.ModoMantenimientoResponse;
import com.mypelink.backend.shared.application.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionPublicaController {

    private final ConfiguracionService configuracionService;

    @GetMapping("/estado")
    public ResponseEntity<ModoMantenimientoResponse> obtenerEstado() {
        return ResponseEntity.ok(configuracionService.obtenerModoMantenimiento());
    }
}