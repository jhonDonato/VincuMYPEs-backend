package com.mypelink.backend.comunicacion.application.service;

import com.mypelink.backend.comunicacion.application.dto.*;
import com.mypelink.backend.comunicacion.application.service.ChatGrupalService;
import com.mypelink.backend.comunicacion.domain.model.*;
import com.mypelink.backend.comunicacion.domain.repository.*;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoConversacion;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
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
class ChatGrupalServiceTest {

    @Mock private ChatGrupoRepository chatGrupoRepository;
    @Mock private MensajeGrupoRepository mensajeGrupoRepository;
    @Mock private MiembroChatGrupoRepository miembroChatGrupoRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ConversacionRepository conversacionRepository;

    @InjectMocks
    private ChatGrupalService chatGrupalService;

    private Role roleEst;
    private Role roleMype;
    private Usuario usuarioEst;
    private Usuario usuarioEst2;
    private Usuario usuarioMype;
    private Estudiante estudiante;
    private Estudiante estudiante2;
    private Mype mype;
    private Proyecto proyecto;
    private Postulacion postulacion;
    private Postulacion postulacion2;
    private ChatGrupo chatGrupo;
    private MiembroChatGrupo miembro;
    private MensajeGrupo mensajeGrupo;

    @BeforeEach
    void setUp() {
        roleEst = Role.builder().id(1L).nombre("ROLE_ESTUDIANTE").build();
        roleMype = Role.builder().id(2L).nombre("ROLE_MYPE").build();

        usuarioEst = Usuario.builder().id(1L).nombre("Est1").email("est1@test.com").rol(roleEst).build();
        usuarioEst2 = Usuario.builder().id(2L).nombre("Est2").email("est2@test.com").rol(roleEst).build();
        usuarioMype = Usuario.builder().id(3L).nombre("Mype").email("mype@test.com").rol(roleMype).build();

        estudiante = Estudiante.builder().id(1L).usuario(usuarioEst).build();
        estudiante2 = Estudiante.builder().id(2L).usuario(usuarioEst2).build();
        mype = Mype.builder().id(1L).usuario(usuarioMype).build();

        proyecto = Proyecto.builder().id(1L).titulo("Proyecto Test").mype(mype).build();

        postulacion = Postulacion.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .estado(EstadoPostulacion.CONFIRMADO).build();
        postulacion2 = Postulacion.builder()
                .id(2L).proyecto(proyecto).estudiante(estudiante2)
                .estado(EstadoPostulacion.CONFIRMADO).build();

        chatGrupo = ChatGrupo.builder()
                .id(1L).proyecto(proyecto).tipo(TipoConversacion.EQUIPO)
                .nombre("Equipo - Proyecto Test").build();

        miembro = MiembroChatGrupo.builder()
                .id(1L).chatGrupo(chatGrupo).usuario(usuarioEst).build();

        mensajeGrupo = MensajeGrupo.builder()
                .id(1L).chatGrupo(chatGrupo).remitente(usuarioEst)
                .mensaje("Hola grupo").fechaEnvio(LocalDateTime.now()).build();
    }

    @Test
    void crearChatsParaProyecto_WithSingleStudent_CreatesDirectConversation() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(postulacion));
        when(conversacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.empty());
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(null);

        chatGrupalService.crearChatsParaProyecto(1L);

        verify(conversacionRepository, times(1)).save(any(Conversacion.class));
        verify(chatGrupoRepository, never()).save(any(ChatGrupo.class));
    }

    @Test
    void crearChatsParaProyecto_WithMultipleStudents_CreatesGroupChats() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(postulacion, postulacion2));
        when(chatGrupoRepository.save(any(ChatGrupo.class))).thenReturn(chatGrupo);
        when(miembroChatGrupoRepository.save(any(MiembroChatGrupo.class))).thenReturn(miembro);

        chatGrupalService.crearChatsParaProyecto(1L);

        verify(chatGrupoRepository, times(2)).save(any(ChatGrupo.class));
        verify(conversacionRepository, never()).save(any(Conversacion.class));
    }

    @Test
    void eliminarChatsGrupalesDeProyecto_Success() {
        when(chatGrupoRepository.findByProyectoId(1L)).thenReturn(List.of(chatGrupo));
        when(mensajeGrupoRepository.findByChatGrupoId(1L)).thenReturn(List.of(mensajeGrupo));
        when(miembroChatGrupoRepository.findByChatGrupoIdWithUsuario(1L)).thenReturn(List.of(miembro));

        chatGrupalService.eliminarChatsGrupalesDeProyecto(1L);

        verify(mensajeGrupoRepository, times(1)).deleteAll(anyList());
        verify(miembroChatGrupoRepository, times(1)).deleteAll(anyList());
        verify(chatGrupoRepository, times(1)).deleteAll(anyList());
    }

    @Test
    void misChats_Success() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst));
        when(chatGrupoRepository.findByMiembroUsuarioId(1L)).thenReturn(List.of(chatGrupo));
        when(miembroChatGrupoRepository.findByChatGrupoIdWithUsuario(1L)).thenReturn(List.of(miembro));

        List<ChatGrupoResponse> result = chatGrupalService.misChats("est1@test.com");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void getMensajes_Success() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst));
        when(miembroChatGrupoRepository.existsByChatGrupoIdAndUsuarioId(1L, 1L)).thenReturn(true);
        when(mensajeGrupoRepository.findByChatGrupoId(1L)).thenReturn(List.of(mensajeGrupo));

        List<MensajeGrupoResponse> result = chatGrupalService.getMensajes(1L, "est1@test.com");

        assertFalse(result.isEmpty());
        assertEquals("Hola grupo", result.get(0).mensaje());
    }

    @Test
    void getMensajes_ShouldFail_WhenNotMember() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst));
        when(miembroChatGrupoRepository.existsByChatGrupoIdAndUsuarioId(1L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> chatGrupalService.getMensajes(1L, "est1@test.com"));
    }

    @Test
    void enviarMensaje_Success() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst));
        when(chatGrupoRepository.findById(1L)).thenReturn(Optional.of(chatGrupo));
        when(miembroChatGrupoRepository.existsByChatGrupoIdAndUsuarioId(1L, 1L)).thenReturn(true);
        when(mensajeGrupoRepository.save(any(MensajeGrupo.class))).thenReturn(mensajeGrupo);
        when(chatGrupoRepository.save(any(ChatGrupo.class))).thenReturn(chatGrupo);

        MensajeGrupoRequest request = new MensajeGrupoRequest("Hola", null);
        MensajeGrupoResponse response = chatGrupalService.enviarMensaje(1L, request, "est1@test.com");

        assertNotNull(response);
        verify(mensajeGrupoRepository, times(1)).save(any(MensajeGrupo.class));
    }

    @Test
    void enviarMensaje_ShouldFail_WhenNotMember() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst));
        when(chatGrupoRepository.findById(1L)).thenReturn(Optional.of(chatGrupo));
        when(miembroChatGrupoRepository.existsByChatGrupoIdAndUsuarioId(1L, 1L)).thenReturn(false);

        MensajeGrupoRequest request = new MensajeGrupoRequest("Hola", null);
        assertThrows(BusinessException.class,
                () -> chatGrupalService.enviarMensaje(1L, request, "est1@test.com"));
    }
}
