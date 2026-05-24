package com.mypelink.backend.comunicacion.infrastructure.rest;

import com.mypelink.backend.comunicacion.application.dto.*;
import com.mypelink.backend.comunicacion.application.service.MensajeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/mensajes")
@RequiredArgsConstructor
public class MensajeController {

    private final MensajeService mensajeService;

    // MYPE ve sus conversaciones
    @GetMapping("/conversaciones")
    @PreAuthorize("hasAnyAuthority('ROLE_MYPE','MYPE')")
    public ResponseEntity<List<ConversacionResponse>> misConversaciones(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mensajeService.misConversaciones(userDetails.getUsername()));
    }
    // ✅ NUEVO - Para ESTUDIANTE
    @GetMapping("/conversaciones/estudiante")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE','ESTUDIANTE')")
    public ResponseEntity<List<ConversacionResponse>> misConversacionesEstudiante(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                mensajeService.misConversacionesEstudiante(userDetails.getUsername())
        );
    }
    // ✅ NUEVO: Crear conversación
    @PostMapping("/conversaciones")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE','ESTUDIANTE','ROLE_MYPE','MYPE')")
    public ResponseEntity<ConversacionResponse> crearConversacion(
            @RequestBody CrearConversacionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mensajeService.crearConversacion(request, userDetails.getUsername()));
    }

    // Mensajes de una conversación (MYPE y Estudiante)
    @GetMapping("/conversaciones/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MensajeResponse>> getMensajes(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mensajeService.getMensajes(id, userDetails.getUsername()));
    }

    // Enviar mensaje
    @PostMapping("/conversaciones/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MensajeResponse> enviar(
            @PathVariable Long id,
            @RequestBody EnviarMensajeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(mensajeService.enviarMensaje(id, request, userDetails.getUsername()));
    }
}