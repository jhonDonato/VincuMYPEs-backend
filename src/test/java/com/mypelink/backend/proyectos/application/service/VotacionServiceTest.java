package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.application.dto.VotacionResponse;
import com.mypelink.backend.proyectos.application.dto.VotarRequest;
import com.mypelink.backend.proyectos.application.service.VotacionService;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.FaseVotacion;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
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
class VotacionServiceTest {

    @Mock private VotacionDelegadoRepository votacionRepository;
    @Mock private VotoDelegadoRepository votoRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private VotacionService votacionService;

    private Role roleEst;
    private Role roleMype;
    private Usuario usuarioEst1;
    private Usuario usuarioEst2;
    private Usuario usuarioEst3;
    private Usuario usuarioMype;
    private Estudiante estudiante1;
    private Estudiante estudiante2;
    private Estudiante estudiante3;
    private Mype mype;
    private Proyecto proyecto;
    private Postulacion post1;
    private Postulacion post2;
    private Postulacion post3;
    private VotacionDelegado votacionActiva;
    private VotacionDelegado votacionCompletada;
    private VotoDelegado voto;

    @BeforeEach
    void setUp() {
        roleEst = Role.builder().id(1L).nombre("ROLE_ESTUDIANTE").build();
        roleMype = Role.builder().id(2L).nombre("ROLE_MYPE").build();

        usuarioEst1 = Usuario.builder().id(1L).nombre("Est1").email("est1@test.com").rol(roleEst).build();
        usuarioEst2 = Usuario.builder().id(2L).nombre("Est2").email("est2@test.com").rol(roleEst).build();
        usuarioEst3 = Usuario.builder().id(3L).nombre("Est3").email("est3@test.com").rol(roleEst).build();
        usuarioMype = Usuario.builder().id(4L).nombre("Mype").email("mype@test.com").rol(roleMype).build();

        estudiante1 = Estudiante.builder().id(1L).usuario(usuarioEst1).build();
        estudiante2 = Estudiante.builder().id(2L).usuario(usuarioEst2).build();
        estudiante3 = Estudiante.builder().id(3L).usuario(usuarioEst3).build();
        mype = Mype.builder().id(1L).usuario(usuarioMype).build();

        proyecto = Proyecto.builder().id(1L).titulo("Proyecto Test").mype(mype).build();

        post1 = Postulacion.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante1)
                .estado(EstadoPostulacion.CONFIRMADO).build();
        post2 = Postulacion.builder()
                .id(2L).proyecto(proyecto).estudiante(estudiante2)
                .estado(EstadoPostulacion.CONFIRMADO).build();
        post3 = Postulacion.builder()
                .id(3L).proyecto(proyecto).estudiante(estudiante3)
                .estado(EstadoPostulacion.CONFIRMADO).build();

        votacionActiva = VotacionDelegado.builder()
                .id(1L).proyecto(proyecto)
                .fechaLimite(LocalDateTime.now().plusHours(48))
                .estado(FaseVotacion.EN_VOTACION)
                .build();

        votacionCompletada = VotacionDelegado.builder()
                .id(2L).proyecto(proyecto)
                .fechaLimite(LocalDateTime.now())
                .estado(FaseVotacion.COMPLETADA)
                .postulacionGanadora(post1)
                .build();

        voto = VotoDelegado.builder()
                .id(1L).votacion(votacionActiva)
                .votante(estudiante1).candidato(estudiante2)
                .build();
    }

    // ══════════════════════════════════════
    // INICIAR VOTACIÓN
    // ══════════════════════════════════════

    @Test
    void iniciarVotacion_With1Student_AutoDelegate() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(votacionRepository.existsByProyectoId(1L)).thenReturn(false);
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(post1));
        when(postulacionRepository.save(any(Postulacion.class))).thenReturn(post1);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(votacionRepository.save(any(VotacionDelegado.class))).thenReturn(votacionCompletada);

        VotacionResponse response = votacionService.iniciarVotacion(1L);

        assertTrue(post1.getEsDelegado());
        assertEquals(FaseVotacion.COMPLETADA, proyecto.getFaseVotacion());
    }

    @Test
    void iniciarVotacion_With2Students_RandomDelegate() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(votacionRepository.existsByProyectoId(1L)).thenReturn(false);
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(post1, post2));
        when(postulacionRepository.save(any(Postulacion.class))).thenReturn(post1);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(votacionRepository.save(any(VotacionDelegado.class))).thenReturn(votacionCompletada);

        VotacionResponse response = votacionService.iniciarVotacion(1L);

        assertEquals(FaseVotacion.COMPLETADA, proyecto.getFaseVotacion());
        verify(postulacionRepository, atLeastOnce()).save(any(Postulacion.class));
    }

    @Test
    void iniciarVotacion_With3Students_CreatesVoting() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(votacionRepository.existsByProyectoId(1L)).thenReturn(false);
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(post1, post2, post3));
        when(votacionRepository.save(any(VotacionDelegado.class))).thenReturn(votacionActiva);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        VotacionResponse response = votacionService.iniciarVotacion(1L);

        assertEquals(FaseVotacion.EN_VOTACION, proyecto.getFaseVotacion());
        verify(votacionRepository, times(1)).save(any(VotacionDelegado.class));
    }

    @Test
    void iniciarVotacion_ShouldFail_WhenAlreadyExists() {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(votacionRepository.existsByProyectoId(1L)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> votacionService.iniciarVotacion(1L));
    }

    // ══════════════════════════════════════
    // VOTAR
    // ══════════════════════════════════════

    @Test
    void votar_Success() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst1));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante1));
        when(votacionRepository.findActivaByProyectoId(1L)).thenReturn(Optional.of(votacionActiva));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(post1));
        when(votoRepository.existsByVotacionIdAndVotanteId(1L, 1L)).thenReturn(false);
        when(estudianteRepository.findById(2L)).thenReturn(Optional.of(estudiante2));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 2L)).thenReturn(Optional.of(post2));
        when(votoRepository.save(any(VotoDelegado.class))).thenReturn(voto);
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(post1, post2, post3));
        when(votoRepository.findByVotacionIdWithDetails(1L)).thenReturn(List.of(voto));

        VotarRequest request = new VotarRequest(2L);
        VotacionResponse response = votacionService.votar(1L, request, "est1@test.com");

        assertNotNull(response);
        verify(votoRepository, times(1)).save(any(VotoDelegado.class));
    }

    @Test
    void votar_ShouldFail_WhenAlreadyVoted() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst1));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante1));
        when(votacionRepository.findActivaByProyectoId(1L)).thenReturn(Optional.of(votacionActiva));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(post1));
        when(votoRepository.existsByVotacionIdAndVotanteId(1L, 1L)).thenReturn(true);

        VotarRequest request = new VotarRequest(2L);
        assertThrows(BusinessException.class,
                () -> votacionService.votar(1L, request, "est1@test.com"));
    }

    @Test
    void votar_ShouldFail_WhenExpired() {
        votacionActiva.setFechaLimite(LocalDateTime.now().minusHours(1));

        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst1));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante1));
        when(votacionRepository.findActivaByProyectoId(1L)).thenReturn(Optional.of(votacionActiva));

        VotarRequest request = new VotarRequest(2L);
        assertThrows(BusinessException.class,
                () -> votacionService.votar(1L, request, "est1@test.com"));
    }

    // ══════════════════════════════════════
    // FINALIZAR VOTACIÓN
    // ══════════════════════════════════════

    @Test
    void finalizarVotacion_WithWinner_Success() {
        Postulacion postGanadora = post1;
        postGanadora.setEsDelegado(false);

        votacionActiva.setPostulacionGanadora(null);

        when(votoRepository.findByVotacionIdWithDetails(1L)).thenReturn(List.of(
                VotoDelegado.builder().id(1L).votacion(votacionActiva)
                        .votante(estudiante1).candidato(estudiante1)
                        .candidato(estudiante1).build(),
                VotoDelegado.builder().id(2L).votacion(votacionActiva)
                        .votante(estudiante2).candidato(estudiante1)
                        .candidato(estudiante1).build()
        ));
        when(postulacionRepository.findByProyectoId(1L)).thenReturn(List.of(post1, post2, post3));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(post1));
        when(postulacionRepository.save(any(Postulacion.class))).thenReturn(post1);
        when(votacionRepository.save(any(VotacionDelegado.class))).thenReturn(votacionActiva);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(post1, post2, post3));

        VotacionResponse response = votacionService.finalizarVotacion(votacionActiva);

        assertTrue(post1.getEsDelegado());
        assertEquals(FaseVotacion.COMPLETADA, votacionActiva.getEstado());
    }

    @Test
    void finalizarVotacion_With3WayTie_RestartsVoting() {
        when(votoRepository.findByVotacionIdWithDetails(1L)).thenReturn(List.of(
                VotoDelegado.builder().id(1L).votacion(votacionActiva)
                        .votante(estudiante1).candidato(estudiante1).build(),
                VotoDelegado.builder().id(2L).votacion(votacionActiva)
                        .votante(estudiante2).candidato(estudiante2).build(),
                VotoDelegado.builder().id(3L).votacion(votacionActiva)
                        .votante(estudiante3).candidato(estudiante3).build()
        ));
        when(votacionRepository.save(any(VotacionDelegado.class))).thenReturn(votacionActiva);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of());

        VotacionResponse response = votacionService.finalizarVotacion(votacionActiva);

        assertEquals(FaseVotacion.COMPLETADA, votacionActiva.getEstado());
        verify(votacionRepository, times(2)).save(any(VotacionDelegado.class));
    }

    // ══════════════════════════════════════
    // OBTENER VOTACIÓN
    // ══════════════════════════════════════

    @Test
    void obtenerVotacion_Active_Success() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst1));
        when(votacionRepository.findActivaByProyectoId(1L)).thenReturn(Optional.of(votacionActiva));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante1));
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(post1, post2, post3));
        when(votoRepository.countVotosByCandidato(anyLong(), anyLong())).thenReturn(0L);
        when(votoRepository.findByVotacionIdWithDetails(1L)).thenReturn(List.of());

        VotacionResponse response = votacionService.obtenerVotacion(1L, "est1@test.com");

        assertNotNull(response);
        assertEquals(FaseVotacion.EN_VOTACION, response.estado());
    }

    @Test
    void obtenerVotacion_Completed_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(votacionRepository.findActivaByProyectoId(1L)).thenReturn(Optional.empty());
        when(votacionRepository.findCompletadaByProyectoId(1L)).thenReturn(Optional.of(votacionCompletada));
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(post1, post2, post3));
        when(votoRepository.countVotosByCandidato(anyLong(), anyLong())).thenReturn(0L);
        when(votoRepository.findByVotacionIdWithDetails(2L)).thenReturn(List.of());

        VotacionResponse response = votacionService.obtenerVotacion(1L, "mype@test.com");

        assertNotNull(response);
        assertEquals(FaseVotacion.COMPLETADA, response.estado());
    }

    // ══════════════════════════════════════
    // PROPONERSE COMO CANDIDATO
    // ══════════════════════════════════════

    @Test
    void proponerseComoCandidato_Success() {
        when(usuarioRepository.findByEmailWithRole("est1@test.com")).thenReturn(Optional.of(usuarioEst1));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante1));
        when(votacionRepository.findActivaByProyectoId(1L)).thenReturn(Optional.of(votacionActiva));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(post1));
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(post1, post2, post3));
        when(votoRepository.countVotosByCandidato(anyLong(), anyLong())).thenReturn(0L);
        when(votoRepository.findByVotacionIdWithDetails(1L)).thenReturn(List.of());

        VotacionResponse response = votacionService.proponerseComoCandidato(1L, "est1@test.com");

        assertNotNull(response);
    }
}
