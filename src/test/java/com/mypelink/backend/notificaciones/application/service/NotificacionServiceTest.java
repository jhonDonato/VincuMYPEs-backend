package com.mypelink.backend.notificaciones.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.notificaciones.application.dto.NotificacionResponse;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.notificaciones.domain.model.Notificacion;
import com.mypelink.backend.notificaciones.domain.repository.NotificacionRepository;
import com.mypelink.backend.notificaciones.infrastructure.rest.websocket.NotificationWebSocketService;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private NotificacionRepository notificacionRepository;
    @Mock private NotificationWebSocketService webSocketService;
    @Mock private EmailService emailService;

    @InjectMocks
    private NotificacionService notificacionService;

    private Usuario usuarioEst;
    private Usuario usuarioMype;
    private Notificacion notificacion;

    @BeforeEach
    void setUp() {
        Role roleEst = Role.builder().id(1L).nombre("ESTUDIANTE").build();
        Role roleMype = Role.builder().id(2L).nombre("MYPE").build();
        usuarioEst = Usuario.builder().id(1L).nombre("Est").email("est@test.com").rol(roleEst).build();
        usuarioMype = Usuario.builder().id(2L).nombre("Mype").email("mype@test.com").rol(roleMype).build();

        notificacion = Notificacion.builder()
                .id(1L).usuario(usuarioEst).titulo("Notif Test")
                .mensaje("Mensaje").tipo(TipoNotificacion.PROYECTO)
                .leida(false).fechaCreacion(LocalDateTime.now())
                .urlReferencia("/test").build();
    }

    @Test
    void listarMisNotificaciones_Success() {
        when(notificacionRepository.findByUsuarioEmailOrderByFechaCreacionDesc("est@test.com"))
                .thenReturn(List.of(notificacion));

        List<NotificacionResponse> result = notificacionService.listarMisNotificaciones("est@test.com");

        assertEquals(1, result.size());
        assertEquals("Notif Test", result.get(0).titulo());
    }

    @Test
    void listarNoLeidas_Success() {
        when(notificacionRepository.findByUsuarioEmailAndLeidaFalseOrderByFechaCreacionDesc("est@test.com"))
                .thenReturn(List.of(notificacion));

        List<NotificacionResponse> result = notificacionService.listarNoLeidas("est@test.com");

        assertEquals(1, result.size());
        assertFalse(result.get(0).leida());
    }

    @Test
    void marcarComoLeida_Success() {
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacion));

        NotificacionResponse response = notificacionService.marcarComoLeida(1L, "est@test.com");

        assertTrue(notificacion.getLeida());
        assertNotNull(notificacion.getFechaLectura());
    }

    @Test
    void marcarComoLeida_ShouldFail_WhenWrongUser() {
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacion));

        assertThrows(com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException.class,
                () -> notificacionService.marcarComoLeida(1L, "otro@test.com"));
    }

    @Test
    void crearNotificacion_ForEstudiante_SendsEmail() {
        when(notificacionRepository.save(any(Notificacion.class))).thenReturn(notificacion);
        doNothing().when(webSocketService).enviarNotificacion(anyLong(), any());
        doNothing().when(emailService).enviarCorreoNotificacion(any(), any(), any(), any());

        notificacionService.crearNotificacion(usuarioEst, "Titulo", "Mensaje",
                TipoNotificacion.PROYECTO, "/test");

        verify(emailService, times(1)).enviarCorreoNotificacion(any(), any(), any(), any());
        verify(webSocketService, times(1)).enviarNotificacion(anyLong(), any());
    }

    @Test
    void crearNotificacion_ForMype_DoesNotSendEmail() {
        notificacion.setUsuario(usuarioMype);
        when(notificacionRepository.save(any(Notificacion.class))).thenReturn(notificacion);
        doNothing().when(webSocketService).enviarNotificacion(anyLong(), any());

        notificacionService.crearNotificacion(usuarioMype, "Titulo", "Mensaje",
                TipoNotificacion.PROYECTO, "/test");

        verify(emailService, never()).enviarCorreoNotificacion(any(), any(), any(), any());
        verify(webSocketService, times(1)).enviarNotificacion(anyLong(), any());
    }

    @Test
    void marcarTodasComoLeidas_Success() {
        when(notificacionRepository.findByUsuarioEmailAndLeidaFalseOrderByFechaCreacionDesc("est@test.com"))
                .thenReturn(List.of(notificacion));

        notificacionService.marcarTodasComoLeidas("est@test.com");

        assertTrue(notificacion.getLeida());
        verify(notificacionRepository, times(1)).saveAll(anyList());
    }

    @Test
    void eliminarNotificacion_Success() {
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacion));

        notificacionService.eliminarNotificacion(1L, "est@test.com");

        verify(notificacionRepository, times(1)).delete(notificacion);
    }

    @Test
    void eliminarTodas_Success() {
        when(notificacionRepository.findByUsuarioEmailOrderByFechaCreacionDesc("est@test.com"))
                .thenReturn(List.of(notificacion));

        notificacionService.eliminarTodas("est@test.com");

        verify(notificacionRepository, times(1)).deleteAll(anyList());
    }
}
