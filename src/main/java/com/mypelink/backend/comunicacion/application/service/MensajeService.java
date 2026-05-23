package com.mypelink.backend.comunicacion.application.service;

import com.mypelink.backend.comunicacion.application.dto.*;
import com.mypelink.backend.comunicacion.domain.model.Conversacion;
import com.mypelink.backend.comunicacion.domain.model.Mensaje;
import com.mypelink.backend.comunicacion.domain.repository.ConversacionRepository;
import com.mypelink.backend.comunicacion.domain.repository.MensajeRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MensajeService {

    private final ConversacionRepository conversacionRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final MypeRepository mypeRepository;

    // MYPE ve todas sus conversaciones activas
    @Transactional(readOnly = true)
    public List<ConversacionResponse> misConversaciones(String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        return conversacionRepository.findByMypeUsuarioId(usuario.getId())
                .stream()
                .map(c -> toConversacionResponse(c, usuario.getId()))
                .toList();
    }

    // Obtener mensajes de una conversación
    @Transactional(readOnly = true)
    public List<MensajeResponse> getMensajes(Long conversacionId, String email) {
        var usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        var conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion", conversacionId));

        // Verificar que el usuario pertenece a esta conversación
        boolean esMype = conversacion.getMypeUsuario().getId().equals(usuario.getId());
        boolean esEstudiante = conversacion.getEstudiante().getUsuario().getId().equals(usuario.getId());
        if (!esMype && !esEstudiante) {
            throw new BusinessException("No tienes acceso a esta conversación", HttpStatus.FORBIDDEN);
        }

        // Marcar como leídos los mensajes del otro
        mensajeRepository.findByConversacionId(conversacionId).stream()
                .filter(m -> !m.getRemitente().getId().equals(usuario.getId()) && !m.getLeido())
                .forEach(m -> { m.setLeido(true); mensajeRepository.save(m); });

        return mensajeRepository.findByConversacionId(conversacionId)
                .stream()
                .map(m -> toMensajeResponse(m, usuario.getId()))
                .toList();
    }

    // Enviar mensaje
    @Transactional
    public MensajeResponse enviarMensaje(Long conversacionId, EnviarMensajeRequest request, String email) {
        var usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        var conversacion = conversacionRepository.findById(conversacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversacion", conversacionId));

        boolean esMype = conversacion.getMypeUsuario().getId().equals(usuario.getId());
        boolean esEstudiante = conversacion.getEstudiante().getUsuario().getId().equals(usuario.getId());
        if (!esMype && !esEstudiante) {
            throw new BusinessException("No tienes acceso a esta conversación", HttpStatus.FORBIDDEN);
        }

        var mensaje = mensajeRepository.save(Mensaje.builder()
                .conversacion(conversacion)
                .remitente(usuario)
                .mensaje(request.mensaje())
                .build());

        // Actualizar último mensaje en la conversación
        conversacion.setUltimoMensaje(request.mensaje());
        conversacion.setFechaUltimoMensaje(LocalDateTime.now());
        conversacionRepository.save(conversacion);

        return toMensajeResponse(mensaje, usuario.getId());
    }

    private ConversacionResponse toConversacionResponse(Conversacion c, Long miId) {
        long noLeidos = mensajeRepository
                .countByConversacionIdAndLeidoFalseAndRemitenteIdNot(c.getId(), miId);
        return new ConversacionResponse(
                c.getId(),
                c.getProyecto().getId(),
                c.getProyecto().getTitulo(),
                c.getEstudiante().getId(),
                c.getEstudiante().getUsuario().getNombre(),
                c.getUltimoMensaje(),
                c.getFechaUltimoMensaje(),
                noLeidos
        );
    }

    private MensajeResponse toMensajeResponse(Mensaje m, Long miId) {
        return new MensajeResponse(
                m.getId(),
                m.getRemitente().getId(),
                m.getRemitente().getNombre(),
                m.getMensaje(),
                m.getArchivoAdjunto(),
                m.getFechaEnvio(),
                m.getLeido(),
                m.getRemitente().getId().equals(miId)
        );
    }
}