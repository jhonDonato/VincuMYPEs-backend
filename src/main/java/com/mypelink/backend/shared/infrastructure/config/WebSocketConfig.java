package com.mypelink.backend.shared.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefijo para mensajes que van del servidor al cliente
        config.enableSimpleBroker("/topic", "/user");

        // Prefijo para mensajes que van del cliente al servidor
        config.setApplicationDestinationPrefixes("/app");

        // Configurar broker de usuarios para notificaciones personalizadas
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint al que se conectará el frontend
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173") // URL de tu frontend
                .withSockJS(); // Fallback para navegadores sin WebSocket
    }
}