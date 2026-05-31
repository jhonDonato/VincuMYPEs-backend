package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.proyectos.application.service.AdminArbolDecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/arbol")
@RequiredArgsConstructor
public class AdminArbolDecisionController {

    private final AdminArbolDecisionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminArbolResponse> listarTodo() {
        return ResponseEntity.ok(service.listarTodo());
    }

    @GetMapping("/nodos/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminNodoResponse> obtenerNodo(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerNodo(id));
    }

    @PostMapping("/nodos")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminNodoResponse> crearNodo(@Valid @RequestBody NodoRequest request) {
        return ResponseEntity.ok(service.crearNodo(request));
    }

    @PutMapping("/nodos/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminNodoResponse> actualizarNodo(@PathVariable Long id,
                                                            @Valid @RequestBody NodoRequest request) {
        return ResponseEntity.ok(service.actualizarNodo(id, request));
    }

    @DeleteMapping("/nodos/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminarNodo(@PathVariable Long id) {
        service.eliminarNodo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/nodos/{nodoId}/opciones")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminOpcionResponse> crearOpcion(@PathVariable Long nodoId,
                                                           @Valid @RequestBody OpcionRequest request) {
        return ResponseEntity.ok(service.crearOpcion(nodoId, request));
    }

    @PutMapping("/opciones/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminOpcionResponse> actualizarOpcion(@PathVariable Long id,
                                                                @Valid @RequestBody OpcionRequest request) {
        return ResponseEntity.ok(service.actualizarOpcion(id, request));
    }

    @DeleteMapping("/opciones/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminarOpcion(@PathVariable Long id) {
        service.eliminarOpcion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/validar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ValidacionResponse> validar() {
        return ResponseEntity.ok(service.validar());
    }
}