package com.mypelink.backend.notificaciones.infrastructure.rest.websocket;

import com.mypelink.backend.notificaciones.application.dto.NotificacionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Envía una notificación en tiempo real a un usuario específico
     * @param userId ID del usuario que recibirá la notificación
     * @param notificacion Datos de la notificación
     */
    public void enviarNotificacion(Long userId, NotificacionResponse notificacion) {
        // Enviar a la cola personal del usuario
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notificaciones",
                notificacion
        );

        System.out.println("📤 WebSocket: Notificación enviada a usuario " + userId);
    }

    /**
     * Notifica que hay una actualización (útil para invalidar caché)
     */
    public void notificarActualizacion(Long userId) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/actualizacion",
                "ACTUALIZAR"
        );
    }
}