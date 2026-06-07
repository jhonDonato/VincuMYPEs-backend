package com.mypelink.backend.usuarios.infrastructure.rest;

import com.mypelink.backend.usuarios.application.dto.MypePerfilPublicoResponse;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypes")
@RequiredArgsConstructor
public class MypePublicController {

    private final ProyectoService proyectoService;

    @GetMapping("/{id}/publico")
    public ResponseEntity<MypePerfilPublicoResponse> obtenerPerfilPublico(@PathVariable Long id) {
        return ResponseEntity.ok(proyectoService.obtenerPerfilPublicoMype(id));
    }
}