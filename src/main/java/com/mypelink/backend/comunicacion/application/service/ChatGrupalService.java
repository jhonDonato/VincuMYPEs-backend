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
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;    // ← añadido
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatGrupalService {

    private final ChatGrupoRepository chatGrupoRepository;
    private final MensajeGrupoRepository mensajeGrupoRepository;
    private final MiembroChatGrupoRepository miembroChatGrupoRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConversacionRepository conversacionRepository;


    // ═══════════════════════════════════════════════════════════
    // CREAR CHATS (al iniciar proyecto)
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public void crearChatsParaProyecto(Long proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyectoId, EstadoPostulacion.CONFIRMADO);

        if (confirmados.isEmpty()) return;

        // ✅ Proyecto de 1 solo estudiante → SOLO chat directo (sin grupos)
        if (confirmados.size() == 1) {
            crearConversacionDirecta(proyecto, confirmados.get(0).getEstudiante());
            return;
        }

        // ═══════════════════════════════════════════════════════════
        // 2+ estudiantes → EQUIPO + PROYECTO (sin mensajes de bienvenida)
        // ═══════════════════════════════════════════════════════════

        // 1. Chat de EQUIPO (solo estudiantes)
        ChatGrupo chatEquipo = chatGrupoRepository.save(ChatGrupo.builder()
                .proyecto(proyecto)
                .tipo(TipoConversacion.EQUIPO)
                .nombre("Equipo - " + proyecto.getTitulo())
                .build());

        for (Postulacion p : confirmados) {
            miembroChatGrupoRepository.save(MiembroChatGrupo.builder()
                    .chatGrupo(chatEquipo)
                    .usuario(p.getEstudiante().getUsuario())
                    .build());
        }

        // 2. Chat de PROYECTO (estudiantes + MYPE)
        ChatGrupo chatProyecto = chatGrupoRepository.save(ChatGrupo.builder()
                .proyecto(proyecto)
                .tipo(TipoConversacion.PROYECTO)
                .nombre("Proyecto - " + proyecto.getTitulo())
                .build());

        for (Postulacion p : confirmados) {
            miembroChatGrupoRepository.save(MiembroChatGrupo.builder()
                    .chatGrupo(chatProyecto)
                    .usuario(p.getEstudiante().getUsuario())
                    .build());
        }

        miembroChatGrupoRepository.save(MiembroChatGrupo.builder()
                .chatGrupo(chatProyecto)
                .usuario(proyecto.getMype().getUsuario())
                .build());
    }
    @Transactional
    public void asegurarMiembrosEnChats(Long proyectoId) {
        log.info("🔍 asegurarMiembrosEnChats: proyectoId={}", proyectoId);
        List<ChatGrupo> chats = chatGrupoRepository.findByProyectoId(proyectoId);
        log.info("   Chats encontrados: {}", chats.size());

        if (chats.isEmpty()) {
            log.info("   No hay chats, creando desde cero...");
            crearChatsParaProyecto(proyectoId);
            return;
        }

        List<Postulacion> confirmados = postulacionRepository
                .findByProyectoIdAndEstadoWithDetails(proyectoId, EstadoPostulacion.CONFIRMADO);
        log.info("   Confirmados: {} estudiantes", confirmados.size());

        for (ChatGrupo chat : chats) {
            Set<Long> miembrosActuales = miembroChatGrupoRepository
                    .findByChatGrupoIdWithUsuario(chat.getId())
                    .stream()
                    .map(m -> m.getUsuario().getId())
                    .collect(Collectors.toSet());
            log.info("   Chat '{}' (id={}) tiene {} miembros", chat.getNombre(), chat.getId(), miembrosActuales.size());

            for (Postulacion p : confirmados) {
                if (!miembrosActuales.contains(p.getEstudiante().getUsuario().getId())) {
                    log.info("      ➕ Agregando a estudiante userId={} al chat {}", p.getEstudiante().getUsuario().getId(), chat.getId());
                    miembroChatGrupoRepository.save(MiembroChatGrupo.builder()
                            .chatGrupo(chat)
                            .usuario(p.getEstudiante().getUsuario())
                            .build());
                }
            }
        }
    }
    // ═══════════════════════════════════════════════════════════
    // ELIMINAR CHATS GRUPALES DE UN PROYECTO (EQUIPO + PROYECTO)
    // ═══════════════════════════════════════════════════════════
    @Transactional
    public void eliminarChatsGrupalesDeProyecto(Long proyectoId) {
        // 1. Obtener los chats grupales del proyecto
        List<ChatGrupo> chatsGrupo = chatGrupoRepository.findByProyectoId(proyectoId);

        for (ChatGrupo chat : chatsGrupo) {
            // 1a. Eliminar TODOS los mensajes del chat grupal
            List<MensajeGrupo> mensajes = mensajeGrupoRepository.findByChatGrupoId(chat.getId());
            if (!mensajes.isEmpty()) {
                mensajeGrupoRepository.deleteAll(mensajes);
            }

            // 1b. Eliminar TODOS los miembros del chat grupal
            List<MiembroChatGrupo> miembros = miembroChatGrupoRepository.findByChatGrupoIdWithUsuario(chat.getId());
            if (!miembros.isEmpty()) {
                miembroChatGrupoRepository.deleteAll(miembros);
            }
        }

        // 1c. Eliminar los chats grupales
        if (!chatsGrupo.isEmpty()) {
            chatGrupoRepository.deleteAll(chatsGrupo);
        }
    }

    // ✅ Chat directo para proyectos de 1 solo estudiante (sin mensaje de bienvenida)
    private void crearConversacionDirecta(Proyecto proyecto, Estudiante estudiante) {
        if (conversacionRepository
                .findByProyectoIdAndEstudianteId(proyecto.getId(), estudiante.getId())
                .isPresent()) {
            return;
        }

        conversacionRepository.save(Conversacion.builder()
                .proyecto(proyecto)
                .estudiante(estudiante)
                .mypeUsuario(proyecto.getMype().getUsuario())
                .asunto("Proyecto: " + proyecto.getTitulo())
                .tipo(TipoConversacion.PRIVADA)
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

        if (!miembroChatGrupoRepository.existsByChatGrupoIdAndUsuarioId(chatGrupoId, usuario.getId())) {
            throw new BusinessException("No eres miembro de este chat");
        }

        MensajeGrupo mensaje = mensajeGrupoRepository.save(MensajeGrupo.builder()
                .chatGrupo(chat)
                .remitente(usuario)
                .mensaje(request.mensaje())
                .archivoAdjunto(request.archivoAdjunto())
                .build());

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