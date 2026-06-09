package com.mypelink.backend.ejecucion.infrastructure.websocket;

import com.mypelink.backend.ejecucion.application.service.EntregableService;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class EntregableLockDisconnectHandler {

    private final EntregableService entregableLockService;
    private final UsuarioRepository usuarioRepository;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String username = event.getUser() != null ? event.getUser().getName() : null;
        if (username == null) return;

        usuarioRepository.findByEmailWithRole(username).ifPresent(usuario -> {
            entregableLockService.liberarTodosDeUsuario(usuario.getId());
        });
    }
}