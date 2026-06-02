package com.mypelink.backend.comunicacion.application.service;

import com.mypelink.backend.comunicacion.application.dto.*;
import com.mypelink.backend.comunicacion.domain.model.*;
import com.mypelink.backend.comunicacion.domain.repository.*;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoConversacion;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatGrupalService {

    private final ChatGrupoRepository chatGrupoRepository;
    private final MensajeGrupoRepository mensajeGrupoRepository;
    private final MiembroChatGrupoRepository miembroChatGrupoRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;

    // ═══════════════════════════════════════════════════════════
    // CREAR CHATS GRUPALES (al iniciar proyecto)
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public void crearChatsParaProyecto(Long proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyectoId, EstadoPostulacion.CONFIRMADO);

        // 1. Crear chat de EQUIPO (solo estudiantes)
        ChatGrupo chatEquipo = ChatGrupo.builder()
                .proyecto(proyecto)
                .tipo(TipoConversacion.EQUIPO)
                .nombre("Equipo - " + proyecto.getTitulo())
                .build();
        chatEquipo = chatGrupoRepository.save(chatEquipo);

        // Agregar estudiantes al chat de equipo
        for (Postulacion p : confirmados) {
            miembroChatGrupoRepository.save(MiembroChatGrupo.builder()
                    .chatGrupo(chatEquipo)
                    .usuario(p.getEstudiante().getUsuario())
                    .build());
        }

        // ✅ Mensaje de bienvenida - Usar el primer estudiante como remitente
        Usuario remitenteSistema = confirmados.get(0).getEstudiante().getUsuario();

        mensajeGrupoRepository.save(MensajeGrupo.builder()
                .chatGrupo(chatEquipo)
                .remitente(remitenteSistema) // ← Un estudiante, NO la MYPE
                .mensaje("👋 ¡Bienvenidos al chat de equipo! Aquí pueden coordinar sus entregables. " +
                        "Recuerden votar por su delegado. Este chat es privado, solo visible para el equipo.")
                .build());

        // 2. Crear chat de PROYECTO (estudiantes + MYPE)
        ChatGrupo chatProyecto = ChatGrupo.builder()
                .proyecto(proyecto)
                .tipo(TipoConversacion.PROYECTO)
                .nombre("Proyecto - " + proyecto.getTitulo())
                .build();
        chatProyecto = chatGrupoRepository.save(chatProyecto);

        // Agregar estudiantes
        for (Postulacion p : confirmados) {
            miembroChatGrupoRepository.save(MiembroChatGrupo.builder()
                    .chatGrupo(chatProyecto)
                    .usuario(p.getEstudiante().getUsuario())
                    .build());
        }

        // Agregar MYPE
        miembroChatGrupoRepository.save(MiembroChatGrupo.builder()
                .chatGrupo(chatProyecto)
                .usuario(proyecto.getMype().getUsuario())
                .build());

        // Mensaje de bienvenida
        mensajeGrupoRepository.save(MensajeGrupo.builder()
                .chatGrupo(chatProyecto)
                .remitente(proyecto.getMype().getUsuario())
                .mensaje("👋 ¡Bienvenidos al chat del proyecto! Aquí pueden comunicarse con la MYPE.")
                .build());
    }

    // ═══════════════════════════════════════════════════════════
    // OBTENER CHATS DE UN USUARIO
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<ChatGrupoResponse> misChats(String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(emailUsuario)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        List<ChatGrupo> chats = chatGrupoRepository.findByMiembroUsuarioId(usuario.getId());

        return chats.stream()
                .map(chat -> {
                    List<MiembroChatGrupo> miembros = miembroChatGrupoRepository
                            .findByChatGrupoIdWithUsuario(chat.getId());

                    return new ChatGrupoResponse(
                            chat.getId(),
                            chat.getProyecto().getId(),
                            chat.getProyecto().getTitulo(),
                            chat.getTipo(),
                            chat.getNombre(),
                            chat.getUltimoMensaje(),
                            chat.getFechaUltimoMensaje(),
                            miembros.size(),
                            0
                    );
                })
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // OBTENER MENSAJES DE UN CHAT
    // ═══════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<MensajeGrupoResponse> getMensajes(Long chatGrupoId, String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(emailUsuario)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        // Verificar que el usuario sea miembro
        if (!miembroChatGrupoRepository.existsByChatGrupoIdAndUsuarioId(chatGrupoId, usuario.getId())) {
            throw new BusinessException("No eres miembro de este chat");
        }

        return mensajeGrupoRepository.findByChatGrupoId(chatGrupoId)
                .stream()
                .map(m -> new MensajeGrupoResponse(
                        m.getId(),
                        m.getChatGrupo().getId(),
                        m.getRemitente().getId(),
                        m.getRemitente().getNombre(),
                        m.getRemitente().getRol().getNombre(),
                        m.getMensaje(),
                        m.getArchivoAdjunto(),
                        m.getFechaEnvio(),
                        m.getRemitente().getId().equals(usuario.getId())
                ))
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // ENVIAR MENSAJE A CHAT GRUPAL
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public MensajeGrupoResponse enviarMensaje(Long chatGrupoId, MensajeGrupoRequest request, String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(emailUsuario)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        ChatGrupo chat = chatGrupoRepository.findById(chatGrupoId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatGrupo", chatGrupoId));

        // Verificar membresía
        if (!miembroChatGrupoRepository.existsByChatGrupoIdAndUsuarioId(chatGrupoId, usuario.getId())) {
            throw new BusinessException("No eres miembro de este chat");
        }

        MensajeGrupo mensaje = mensajeGrupoRepository.save(MensajeGrupo.builder()
                .chatGrupo(chat)
                .remitente(usuario)
                .mensaje(request.mensaje())
                .archivoAdjunto(request.archivoAdjunto())
                .build());

        // Actualizar último mensaje del chat
        chat.setUltimoMensaje(request.mensaje());
        chat.setFechaUltimoMensaje(LocalDateTime.now());
        chatGrupoRepository.save(chat);

        return new MensajeGrupoResponse(
                mensaje.getId(),
                chat.getId(),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol().getNombre(),
                mensaje.getMensaje(),
                mensaje.getArchivoAdjunto(),
                mensaje.getFechaEnvio(),
                true
        );
    }
}