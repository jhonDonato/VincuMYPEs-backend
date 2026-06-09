package com.mypelink.backend.calificaciones.application.service;

import com.mypelink.backend.calificaciones.application.dto.CalificacionPendienteResponse;
import com.mypelink.backend.calificaciones.application.dto.CrearCalificacionRequest;
import com.mypelink.backend.calificaciones.application.dto.RatingResponse;
import com.mypelink.backend.calificaciones.application.service.CalificacionService;
import com.mypelink.backend.calificaciones.domain.model.Calificacion;
import com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalificacionServiceTest {

    @Mock private CalificacionRepository calificacionRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MypeRepository mypeRepository;
    @Mock private EstudianteRepository estudianteRepository;

    @InjectMocks
    private CalificacionService calificacionService;

    private Role roleMype;
    private Role roleEst;
    private Role roleAdmin;
    private Usuario usuarioMype;
    private Usuario usuarioEst;
    private Usuario usuarioAdmin;
    private Mype mype;
    private Estudiante estudiante;
    private Proyecto proyecto;
    private Postulacion postulacion;

    @BeforeEach
    void setUp() {
        roleMype = Role.builder().id(1L).nombre("ROLE_MYPE").build();
        roleEst = Role.builder().id(2L).nombre("ROLE_ESTUDIANTE").build();
        roleAdmin = Role.builder().id(3L).nombre("ROLE_ADMIN").build();

        usuarioMype = Usuario.builder().id(1L).nombre("Mype").email("mype@test.com").rol(roleMype).build();
        usuarioEst = Usuario.builder().id(2L).nombre("Est").email("est@test.com").rol(roleEst).build();
        usuarioAdmin = Usuario.builder().id(3L).nombre("Admin").email("admin@test.com").rol(roleAdmin).build();

        mype = Mype.builder().id(1L).usuario(usuarioMype).nombreComercial("MYPE SAS").build();
        estudiante = Estudiante.builder().id(1L).usuario(usuarioEst).build();

        proyecto = Proyecto.builder()
                .id(1L).titulo("Proyecto Test").mype(mype)
                .estado(WorkflowEstado.COMPLETADO).build();

        postulacion = Postulacion.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .estado(EstadoPostulacion.CONFIRMADO).build();
    }

    @Test
    void crear_MypeToStudent_Success() {
        CrearCalificacionRequest request = new CrearCalificacionRequest(1L, 2L, 5);

        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioEst));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(postulacionRepository.existsByProyectoIdAndEstudianteUsuarioIdAndEstado(1L, 2L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(true);
        when(calificacionRepository.existsByProyectoIdAndCalificadorIdAndCalificadoId(1L, 1L, 2L))
                .thenReturn(false);

        calificacionService.crear(request, "mype@test.com");

        verify(calificacionRepository, times(1)).save(any(Calificacion.class));
    }

    @Test
    void crear_StudentToMype_Success() {
        CrearCalificacionRequest request = new CrearCalificacionRequest(1L, 1L, 4);

        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMype));
        when(postulacionRepository.existsByProyectoIdAndEstudianteUsuarioIdAndEstado(1L, 2L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(true);
        when(calificacionRepository.existsByProyectoIdAndCalificadorIdAndCalificadoId(1L, 2L, 1L))
                .thenReturn(false);

        calificacionService.crear(request, "est@test.com");

        verify(calificacionRepository, times(1)).save(any(Calificacion.class));
    }

    @Test
    void crear_ShouldFail_WhenNotCompletado() {
        proyecto.setEstado(WorkflowEstado.PENDIENTE);
        CrearCalificacionRequest request = new CrearCalificacionRequest(1L, 2L, 5);

        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        assertThrows(BusinessException.class,
                () -> calificacionService.crear(request, "mype@test.com"));
    }

    @Test
    void crear_ShouldFail_WhenSelfRating() {
        CrearCalificacionRequest request = new CrearCalificacionRequest(1L, 1L, 5);

        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMype));

        assertThrows(BusinessException.class,
                () -> calificacionService.crear(request, "mype@test.com"));
    }

    @Test
    void crear_ShouldFail_WhenDuplicate() {
        CrearCalificacionRequest request = new CrearCalificacionRequest(1L, 2L, 5);

        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioEst));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(postulacionRepository.existsByProyectoIdAndEstudianteUsuarioIdAndEstado(1L, 2L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(true);
        when(calificacionRepository.existsByProyectoIdAndCalificadorIdAndCalificadoId(1L, 1L, 2L))
                .thenReturn(true);

        assertThrows(BusinessException.class,
                () -> calificacionService.crear(request, "mype@test.com"));
    }

    @Test
    void obtenerPendientes_AsMype_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findByMypeIdAndEstado(1L, WorkflowEstado.COMPLETADO))
                .thenReturn(List.of(proyecto));
        when(postulacionRepository.findByProyectoIdAndEstado(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(postulacion));
        when(calificacionRepository.existsByProyectoIdAndCalificadorIdAndCalificadoId(1L, 1L, 2L))
                .thenReturn(false);

        List<CalificacionPendienteResponse> result = calificacionService.obtenerPendientes("mype@test.com");

        assertFalse(result.isEmpty());
        assertEquals("ESTUDIANTE", result.get(0).tipoCalificado());
    }

    @Test
    void obtenerPendientes_AsStudent_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(postulacionRepository.findByEstudianteUsuarioIdAndEstado(2L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(postulacion));
        when(calificacionRepository.existsByProyectoIdAndCalificadorIdAndCalificadoId(1L, 2L, 1L))
                .thenReturn(false);

        List<CalificacionPendienteResponse> result = calificacionService.obtenerPendientes("est@test.com");

        assertFalse(result.isEmpty());
        assertEquals("MYPE", result.get(0).tipoCalificado());
    }

    @Test
    void obtenerRating_AsAdmin_Success() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMype));
        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(usuarioAdmin));
        when(calificacionRepository.promedioDeUsuario(1L)).thenReturn(4.5);
        when(calificacionRepository.cantidadDeUsuario(1L)).thenReturn(10L);

        RatingResponse response = calificacionService.obtenerRating(1L, "admin@test.com");

        assertEquals(4.5, response.promedio());
        assertEquals(10, response.cantidad());
    }

    @Test
    void obtenerRating_ShouldFail_WhenNotPermitted() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMype));
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));

        assertThrows(BusinessException.class,
                () -> calificacionService.obtenerRating(1L, "mype@test.com"));
    }
}
