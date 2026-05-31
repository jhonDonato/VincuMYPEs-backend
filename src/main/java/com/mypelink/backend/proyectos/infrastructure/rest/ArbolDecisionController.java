package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.ArbolDecisionResponse;
import com.mypelink.backend.proyectos.application.service.ArbolDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arbol-decision")
@RequiredArgsConstructor
public class ArbolDecisionController {

    private final ArbolDecisionService arbolDecisionService;

    @GetMapping("/activo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ArbolDecisionResponse> obtenerActivo(@RequestParam(defaultValue = "CORTO") String plazo) {
        return ResponseEntity.ok(arbolDecisionService.obtenerArbolActivo(plazo));
    }
}