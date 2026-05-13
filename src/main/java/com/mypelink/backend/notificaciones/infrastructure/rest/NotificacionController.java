package com.mypelink.backend.notificaciones.infrastructure.rest;

import com.mypelink.backend.notificaciones.application.dto.NotificacionResponse;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificacionService.listarMisNotificaciones(userDetails.getUsername()));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<List<NotificacionResponse>> listarNoLeidas(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificacionService.listarNoLeidas(userDetails.getUsername()));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarComoLeida(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificacionService.marcarComoLeida(id, userDetails.getUsername()));
    }
}
