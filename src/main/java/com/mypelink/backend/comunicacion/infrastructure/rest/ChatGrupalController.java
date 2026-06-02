package com.mypelink.backend.comunicacion.infrastructure.rest;

import com.mypelink.backend.comunicacion.application.dto.*;
import com.mypelink.backend.comunicacion.application.service.ChatGrupalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/chat")
@RequiredArgsConstructor
public class ChatGrupalController {

    private final ChatGrupalService chatGrupalService;

    // ═══════════════════════════════════════════════════════════
    // OBTENER CHATS DEL PROYECTO (equipo y proyecto)
    // ═══════════════════════════════════════════════════════════
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatGrupoResponse>> misChats(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatGrupalService.misChats(userDetails.getUsername()));
    }

    // ═══════════════════════════════════════════════════════════
    // OBTENER MENSAJES DE UN CHAT GRUPAL
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/{chatId}/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MensajeGrupoResponse>> getMensajes(
            @PathVariable Long proyectoId,
            @PathVariable Long chatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                chatGrupalService.getMensajes(chatId, userDetails.getUsername())
        );
    }

    // ═══════════════════════════════════════════════════════════
    // ENVIAR MENSAJE A CHAT GRUPAL
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/{chatId}/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MensajeGrupoResponse> enviarMensaje(
            @PathVariable Long proyectoId,
            @PathVariable Long chatId,
            @Valid @RequestBody MensajeGrupoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                chatGrupalService.enviarMensaje(chatId, request, userDetails.getUsername())
        );
    }

}