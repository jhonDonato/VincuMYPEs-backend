package com.mypelink.backend.comunicacion.application.service;

import com.mypelink.backend.comunicacion.application.dto.*;
import com.mypelink.backend.comunicacion.application.service.MensajeService;
import com.mypelink.backend.comunicacion.domain.model.Conversacion;
import com.mypelink.backend.comunicacion.domain.model.Mensaje;
import com.mypelink.backend.comunicacion.domain.repository.ConversacionRepository;
import com.mypelink.backend.comunicacion.domain.repository.MensajeRepository;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.TipoConversacion;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
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
class MensajeServiceTest {

    @Mock private ConversacionRepository conversacionRepository;
    @Mock private MensajeRepository mensajeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MypeRepository mypeRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ProyectoRepository proyectoRepository;

    @InjectMocks
    private MensajeService mensajeService;

    private Role roleEst;
    private Role roleMype;
    private Usuario usuarioEst;
    private Usuario usuarioMype;
    private Estudiante estudiante;
    private Mype mype;
    private Proyecto proyecto;
    private Conversacion conversacion;
    private Mensaje mensaje;

    @BeforeEach
    void setUp() {
        roleEst = Role.builder().id(1L).nombre("ROLE_ESTUDIANTE").build();
        roleMype = Role.builder().id(2L).nombre("ROLE_MYPE").build();

        usuarioEst = Usuario.builder().id(1L).nombre("Est").email("est@test.com").rol(roleEst).build();
        usuarioMype = Usuario.builder().id(2L).nombre("Mype").email("mype@test.com").rol(roleMype).build();

        estudiante = Estudiante.builder().id(1L).usuario(usuarioEst).build();
        mype = Mype.builder().id(1L).usuario(usuarioMype).nombreComercial("MYPE SAS").build();

        proyecto = Proyecto.builder().id(1L).titulo("Proyecto Test").mype(mype).build();

        conversacion = Conversacion.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .mypeUsuario(usuarioMype).tipo(TipoConversacion.PRIVADA)
                .asunto("Proyecto: Proyecto Test").build();

        mensaje = Mensaje.builder()
                .id(1L).conversacion(conversacion).remitente(usuarioEst)
                .mensaje("Hola").fechaEnvio(LocalDateTime.now()).build();
    }

    @Test
    void crearConversacion_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.empty());
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(conversacion);
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(mensaje);

        CrearConversacionRequest request = new CrearConversacionRequest(1L, "Hola");
        ConversacionResponse response = mensajeService.crearConversacion(request, "est@test.com");

        assertNotNull(response);
        verify(conversacionRepository, times(2)).save(any(Conversacion.class));
    }

    @Test
    void crearConversacion_ReturnsExisting_WhenAlreadyExists() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(conversacion));

        CrearConversacionRequest request = new CrearConversacionRequest(1L, "Hola");
        ConversacionResponse response = mensajeService.crearConversacion(request, "est@test.com");

        assertNotNull(response);
        verify(conversacionRepository, never()).save(any(Conversacion.class));
    }

    @Test
    void crearConversacion_ShouldFail_WhenNotStudent() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        CrearConversacionRequest request = new CrearConversacionRequest(1L, "Hola");
        assertThrows(BusinessException.class,
                () -> mensajeService.crearConversacion(request, "mype@test.com"));
    }

    @Test
    void misConversaciones_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(conversacionRepository.findByMypeUsuarioId(2L)).thenReturn(List.of(conversacion));
        when(mensajeRepository.countByConversacionIdAndLeidoFalseAndRemitenteIdNot(anyLong(), anyLong())).thenReturn(0L);

        List<ConversacionResponse> result = mensajeService.misConversaciones("mype@test.com");

        assertFalse(result.isEmpty());
    }

    @Test
    void misConversacionesEstudiante_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(conversacionRepository.findByEstudianteId(1L)).thenReturn(List.of(conversacion));
        when(mensajeRepository.countByConversacionIdAndLeidoFalseAndRemitenteIdNot(anyLong(), anyLong())).thenReturn(0L);

        List<ConversacionResponse> result = mensajeService.misConversacionesEstudiante("est@test.com");

        assertFalse(result.isEmpty());
    }

    @Test
    void getMensajes_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(conversacion));
        when(mensajeRepository.findByConversacionId(1L)).thenReturn(List.of(mensaje));

        List<MensajeResponse> result = mensajeService.getMensajes(1L, "est@test.com");

        assertFalse(result.isEmpty());
    }

    @Test
    void getMensajes_ShouldFail_WhenNotParticipant() {
        Usuario otro = Usuario.builder().id(99L).nombre("Otro").email("otro@test.com").build();
        when(usuarioRepository.findByEmailWithRole("otro@test.com")).thenReturn(Optional.of(otro));
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(conversacion));

        assertThrows(BusinessException.class,
                () -> mensajeService.getMensajes(1L, "otro@test.com"));
    }

    @Test
    void enviarMensaje_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(conversacionRepository.findById(1L)).thenReturn(Optional.of(conversacion));
        when(mensajeRepository.save(any(Mensaje.class))).thenReturn(mensaje);
        when(conversacionRepository.save(any(Conversacion.class))).thenReturn(conversacion);

        EnviarMensajeRequest request = new EnviarMensajeRequest("Hola de nuevo");
        MensajeResponse response = mensajeService.enviarMensaje(1L, request, "est@test.com");

        assertNotNull(response);
        verify(mensajeRepository, times(1)).save(any(Mensaje.class));
    }

    @Test
    void eliminarConversacionesDirectasDeProyecto_Success() {
        when(conversacionRepository.findByProyectoId(1L)).thenReturn(List.of(conversacion));
        when(mensajeRepository.findByConversacionId(1L)).thenReturn(List.of(mensaje));

        mensajeService.eliminarConversacionesDirectasDeProyecto(1L);

        verify(mensajeRepository, times(1)).deleteAll(anyList());
        verify(conversacionRepository, times(1)).deleteAll(anyList());
    }
}
