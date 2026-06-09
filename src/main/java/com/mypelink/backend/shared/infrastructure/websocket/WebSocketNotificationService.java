package com.mypelink.backend.shared.infrastructure.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Envía un mensaje a un usuario específico (por su userId).
     */
    public void sendToUser(Long userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), destination, payload);
    }
}