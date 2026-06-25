package com.mypelink.backend.notificaciones.application.service;

import com.mypelink.backend.notificaciones.application.dto.NotificacionResponse;
import com.mypelink.backend.notificaciones.domain.model.Notificacion;
import com.mypelink.backend.notificaciones.domain.repository.NotificacionRepository;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.notificaciones.infrastructure.rest.websocket.NotificationWebSocketService;
import com.mypelink.backend.auth.recovery.application.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificationWebSocketService webSocketService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarMisNotificaciones(String email) {
        return notificacionRepository.findByUsuarioEmailOrderByFechaCreacionDesc(email)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarNoLeidas(String email) {
        return notificacionRepository.findByUsuarioEmailAndLeidaFalseOrderByFechaCreacionDesc(email)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificacionResponse marcarComoLeida(Long id, String email) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notificacion.getUsuario().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Notificación no encontrada para este usuario");
        }

        if (!notificacion.getLeida()) {
            notificacion.setLeida(true);
            notificacion.setFechaLectura(LocalDateTime.now());
            notificacionRepository.save(notificacion);
        }

        return mapToResponse(notificacion);
    }

    @Transactional
    public void crearNotificacion(Usuario usuario, String titulo, String mensaje, TipoNotificacion tipo, String urlReferencia) {
        Notificacion notificacion = Notificacion.builder()
                .usuario(usuario)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(tipo)
                .urlReferencia(urlReferencia)
                .leida(false)
                .fechaCreacion(LocalDateTime.now())
                .build();
        notificacion = notificacionRepository.save(notificacion);

        // Emitir por WebSocket
        NotificacionResponse response = mapToResponse(notificacion);
        webSocketService.enviarNotificacion(usuario.getId(), response);

        // ✅ ENVIAR CORREO A TODOS LOS USUARIOS (no solo estudiantes)
        try {
            String nombreUsuario = usuario.getNombre() != null ? usuario.getNombre() : "Usuario";
            emailService.enviarCorreoNotificacion(
                    usuario.getEmail(),
                    titulo,
                    mensaje,
                    nombreUsuario
            );
            log.info("📧 Correo enviado a: {} (rol: {})", usuario.getEmail(), usuario.getRol().getNombre());
        } catch (Exception e) {
            log.error("⚠️ Error enviando correo a {}: {}", usuario.getEmail(), e.getMessage());
        }
    }

    private NotificacionResponse mapToResponse(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.getUrlReferencia(),
                notificacion.getLeida(),
                notificacion.getFechaCreacion(),
                notificacion.getFechaLectura()
        );
    }

    @Transactional
    public void marcarTodasComoLeidas(String email) {
        List<Notificacion> noLeidas = notificacionRepository
                .findByUsuarioEmailAndLeidaFalseOrderByFechaCreacionDesc(email);

        noLeidas.forEach(n -> {
            n.setLeida(true);
            n.setFechaLectura(LocalDateTime.now());
        });
        notificacionRepository.saveAll(noLeidas);
    }

    @Transactional
    public void eliminarNotificacion(Long id, String email) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notificacion.getUsuario().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Notificación no encontrada para este usuario");
        }
        notificacionRepository.delete(notificacion);
    }

    @Transactional
    public void eliminarTodas(String email) {
        List<Notificacion> todas = notificacionRepository
                .findByUsuarioEmailOrderByFechaCreacionDesc(email);
        notificacionRepository.deleteAll(todas);
    }
}