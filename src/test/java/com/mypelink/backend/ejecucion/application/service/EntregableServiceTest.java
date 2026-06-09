package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.ejecucion.application.dto.EntregableResponse;
import com.mypelink.backend.ejecucion.application.dto.RevisarEntregableRequest;
import com.mypelink.backend.ejecucion.application.service.EntregableService;
import com.mypelink.backend.ejecucion.domain.model.Entregable;
import com.mypelink.backend.ejecucion.domain.repository.EntregableRepository;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoEntregable;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
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
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntregableServiceTest {

    @Mock private EntregableRepository entregableRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private MypeRepository mypeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private S3Service s3Service;
    @Mock private ProyectoService proyectoService;
    @Mock private EmailService emailService;

    @InjectMocks
    private EntregableService entregableService;

    private Usuario usuarioMype;
    private Usuario usuarioEstudiante;
    private Mype mype;
    private Proyecto proyecto;
    private Estudiante estudiante;
    private Entregable entregable;
    private Postulacion postulacion;

    @BeforeEach
    void setUp() {
        Role roleMype = Role.builder().id(1L).nombre("ROLE_MYPE").build();
        Role roleEst = Role.builder().id(2L).nombre("ROLE_ESTUDIANTE").build();

        usuarioMype = Usuario.builder().id(1L).nombre("MYPE User").email("mype@test.com").rol(roleMype).build();
        usuarioEstudiante = Usuario.builder().id(2L).nombre("Estudiante User").email("est@test.com").rol(roleEst).build();

        mype = Mype.builder().id(1L).usuario(usuarioMype).nombreComercial("MYPE SAS").build();

        proyecto = Proyecto.builder()
                .id(1L).titulo("Proyecto Test").mype(mype)
                .estado(WorkflowEstado.EN_DESARROLLO)
                .entregablesSugeridos("• Entregable 1\n• Entregable 2")
                .build();

        estudiante = Estudiante.builder().id(1L).usuario(usuarioEstudiante).limiteProyectos(3).build();

        postulacion = Postulacion.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .estado(EstadoPostulacion.CONFIRMADO)
                .esDelegado(true)
                .build();

        entregable = Entregable.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .titulo("Entregable 1").descripcion("Desc")
                .archivo("https://s3.test/doc.pdf")
                .estado(EstadoEntregable.PENDIENTE)
                .fechaEntrega(LocalDateTime.now())
                .subidoPor(usuarioEstudiante)
                .build();
    }

    @Test
    void subir_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEstudiante));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(postulacion));
        when(s3Service.subirEntregablePdf(any())).thenReturn("https://s3.test/upload.pdf");
        when(entregableRepository.save(any(Entregable.class))).thenReturn(entregable);
        doNothing().when(emailService).enviarCorreoNotificacion(any(), any(), any(), any());
        doNothing().when(notificacionService).crearNotificacion(any(), any(), any(), any(), any());

        EntregableResponse response = entregableService.subir(
                1L, "Entregable 1", "Desc", null, "est@test.com");

        assertNotNull(response);
        verify(entregableRepository, times(1)).save(any(Entregable.class));
    }

    @Test
    void subir_ShouldFail_WhenNotDelegado() {
        postulacion.setEsDelegado(false);

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEstudiante));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(postulacion));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> entregableService.subir(1L, "Entregable", "Desc", null, "est@test.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void subir_ShouldFail_WhenNotConfirmed() {
        postulacion.setEstado(EstadoPostulacion.PENDIENTE);

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEstudiante));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(postulacion));

        assertThrows(BusinessException.class,
                () -> entregableService.subir(1L, "Entregable", "Desc", null, "est@test.com"));
    }

    @Test
    void listarPorProyecto_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(entregableRepository.findByProyectoIdWithDetails(1L)).thenReturn(List.of(entregable));

        List<EntregableResponse> responses = entregableService.listarPorProyecto(1L, "mype@test.com");

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
    }

    @Test
    void listarPorProyecto_ShouldFail_WhenNotMypeOwner() {
        Mype otherMype = Mype.builder().id(99L).usuario(usuarioMype).build();

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(otherMype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        assertThrows(BusinessException.class,
                () -> entregableService.listarPorProyecto(1L, "mype@test.com"));
    }

    @Test
    void revisar_Aprobar_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
        when(entregableRepository.save(any(Entregable.class))).thenReturn(entregable);
        doNothing().when(notificacionService).crearNotificacion(any(), any(), any(), any(), any());

        RevisarEntregableRequest request = new RevisarEntregableRequest(EstadoEntregable.APROBADO, "Bien");
        EntregableResponse response = entregableService.revisar(1L, 1L, request, "mype@test.com");

        assertNotNull(response);
        verify(entregableRepository, times(1)).save(any(Entregable.class));
    }

    @Test
    void revisar_AutoCompletaProyecto_WhenLastEntregable() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
        when(entregableRepository.save(any(Entregable.class))).thenReturn(entregable);
        doNothing().when(notificacionService).crearNotificacion(any(), any(), any(), any(), any());
        when(entregableRepository.findByProyectoId(1L)).thenReturn(List.of(entregable));
        when(proyectoService.completarProyecto(1L, "mype@test.com")).thenReturn(null);

        RevisarEntregableRequest request = new RevisarEntregableRequest(EstadoEntregable.APROBADO, "Bien");
        entregableService.revisar(1L, 1L, request, "mype@test.com");

        verify(proyectoService, times(1)).completarProyecto(1L, "mype@test.com");
    }

    @Test
    void revisar_NoCompleta_WhenNotAllAprobados() {
        Entregable otroPendiente = Entregable.builder()
                .id(2L).proyecto(proyecto).estudiante(estudiante)
                .titulo("Entregable 2")
                .estado(EstadoEntregable.PENDIENTE)
                .build();

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));
        when(entregableRepository.save(any(Entregable.class))).thenReturn(entregable);
        doNothing().when(notificacionService).crearNotificacion(any(), any(), any(), any(), any());
        when(entregableRepository.findByProyectoId(1L)).thenReturn(List.of(entregable, otroPendiente));

        RevisarEntregableRequest request = new RevisarEntregableRequest(EstadoEntregable.APROBADO, "Bien");
        entregableService.revisar(1L, 1L, request, "mype@test.com");

        verify(proyectoService, never()).completarProyecto(any(), any());
    }

    @Test
    void eliminar_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEstudiante));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));

        entregableService.eliminar(1L, 1L, "est@test.com");

        verify(entregableRepository, times(1)).delete(entregable);
    }

    @Test
    void eliminar_ShouldFail_WhenAprobado() {
        entregable.setEstado(EstadoEntregable.APROBADO);

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEstudiante));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));

        assertThrows(BusinessException.class,
                () -> entregableService.eliminar(1L, 1L, "est@test.com"));
    }

    @Test
    void eliminar_ShouldFail_WhenNotOwner() {
        Estudiante otroEstudiante = Estudiante.builder().id(99L).build();

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEstudiante));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(otroEstudiante));
        when(entregableRepository.findById(1L)).thenReturn(Optional.of(entregable));

        assertThrows(BusinessException.class,
                () -> entregableService.eliminar(1L, 1L, "est@test.com"));
    }

    @Test
    void misEntregables_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEstudiante));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(entregableRepository.findByEstudianteIdWithDetails(1L)).thenReturn(List.of(entregable));

        List<EntregableResponse> result = entregableService.misEntregables("est@test.com");

        assertEquals(1, result.size());
    }
}
