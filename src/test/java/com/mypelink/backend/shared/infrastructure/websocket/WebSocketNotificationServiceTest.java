package com.mypelink.backend.shared.infrastructure.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketNotificationService webSocketNotificationService;

    @Test
    void sendToUser_Success() {
        webSocketNotificationService.sendToUser(1L, "/queue/test", "payload");

        verify(messagingTemplate, times(1))
                .convertAndSendToUser("1", "/queue/test", "payload");
    }
}
