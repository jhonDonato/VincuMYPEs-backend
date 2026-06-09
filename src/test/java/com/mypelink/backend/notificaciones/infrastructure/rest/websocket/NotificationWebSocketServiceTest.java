package com.mypelink.backend.notificaciones.infrastructure.rest.websocket;

import com.mypelink.backend.notificaciones.application.dto.NotificacionResponse;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationWebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationWebSocketService notificationWebSocketService;

    @Test
    void enviarNotificacion_Success() {
        NotificacionResponse notificacion = new NotificacionResponse(
                1L, TipoNotificacion.MENSAJE, "Título", "Mensaje", null, false, LocalDateTime.now(), null);

        notificationWebSocketService.enviarNotificacion(1L, notificacion);

        verify(messagingTemplate, times(1))
                .convertAndSendToUser("1", "/queue/notificaciones", notificacion);
    }

    @Test
    void notificarActualizacion_Success() {
        notificationWebSocketService.notificarActualizacion(1L);

        verify(messagingTemplate, times(1))
                .convertAndSendToUser("1", "/queue/actualizacion", "ACTUALIZAR");
    }
}
