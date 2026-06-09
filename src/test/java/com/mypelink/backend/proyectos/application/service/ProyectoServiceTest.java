package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.comunicacion.application.service.ChatGrupalService;
import com.mypelink.backend.comunicacion.application.service.MensajeService;
import com.mypelink.backend.ejecucion.domain.model.Entregable;
import com.mypelink.backend.ejecucion.domain.repository.EntregableRepository;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.proyectos.application.service.ProyectoService;
import com.mypelink.backend.proyectos.application.service.VotacionService;
import com.mypelink.backend.proyectos.domain.model.*;
import com.mypelink.backend.proyectos.domain.repository.*;
import com.mypelink.backend.shared.domain.enums.*;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProyectoServiceTest {

    @Mock private MensajeService mensajeService;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private MypeRepository mypeRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EntregableRepository entregableRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private WorkflowHistorialRepository workflowHistorialRepository;
    @Mock private TipoProyectoRepository tipoProyectoRepository;
    @Mock private EntregableTipoRepository entregableTipoRepository;
    @Mock private InsumoTipoRepository insumoTipoRepository;
    @Mock private InsumoProyectoRepository insumoProyectoRepository;
    @Mock private S3Service s3Service;
    @Mock private VotacionService votacionService;
    @Mock private ChatGrupalService chatGrupalService;
    @Mock private EmailService emailService;

    @InjectMocks
    private ProyectoService proyectoService;

    private Role roleAdmin;
    private Role roleMype;
    private Role roleEst;
    private Usuario usuarioAdmin;
    private Usuario usuarioMype;
    private Usuario usuarioEst;
    private Mype mype;
    private Estudiante estudiante;
    private Proyecto proyecto;
    private Postulacion postulacion;
    private Entregable entregable;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(proyectoService, "adminEmails", "admin@test.com");

        roleAdmin = Role.builder().id(1L).nombre("ROLE_ADMIN").build();
        roleMype = Role.builder().id(2L).nombre("ROLE_MYPE").build();
        roleEst = Role.builder().id(3L).nombre("ROLE_ESTUDIANTE").build();

        usuarioAdmin = Usuario.builder().id(99L).nombre("Admin").email("admin@test.com").rol(roleAdmin).build();
        usuarioMype = Usuario.builder().id(1L).nombre("MYPE User").email("mype@test.com").rol(roleMype).build();
        usuarioEst = Usuario.builder().id(2L).nombre("Est User").email("est@test.com").rol(roleEst).build();

        mype = Mype.builder().id(1L).usuario(usuarioMype).nombreComercial("MYPE SAS").build();
        estudiante = Estudiante.builder().id(1L).usuario(usuarioEst).limiteProyectos(3).build();

        proyecto = Proyecto.builder()
                .id(1L).titulo("Proyecto Test").mype(mype)
                .cupos(2).estado(WorkflowEstado.BORRADOR)
                .areaSistemas(AreaSistemas.DESARROLLO_WEB)
                .build();

        postulacion = Postulacion.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .estado(EstadoPostulacion.PENDIENTE)
                .fechaPostulacion(LocalDateTime.now())
                .build();

        entregable = Entregable.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .titulo("Entregable 1")
                .estado(EstadoEntregable.APROBADO)
                .build();
    }

    // ═══════════════════════════════════════════
    // CREAR
    // ═══════════════════════════════════════════

    @Test
    void crear_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto Test", "Desc", "Obj", "Req", "Entregables",
                AreaSistemas.DESARROLLO_WEB, 2, null, null, null, null, null);

        ProyectoResponse response = proyectoService.crear(request, "mype@test.com");

        assertNotNull(response);
        verify(proyectoRepository, times(1)).save(any(Proyecto.class));
    }

    @Test
    void crear_WithTipoProyecto_Success() {
        TipoProyecto tipoProyecto = TipoProyecto.builder().id(1L).nombre("Web App").build();
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(tipoProyectoRepository.findById(1L)).thenReturn(Optional.of(tipoProyecto));
        when(entregableTipoRepository.findByTipoProyectoIdOrderByOrdenAsc(1L)).thenReturn(List.of());
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        CrearProyectoRequest request = new CrearProyectoRequest(
                "Proyecto Test", "Desc", "Obj", "Req", null,
                AreaSistemas.DESARROLLO_WEB, 2, null, null, 1L, null, null);

        proyectoService.crear(request, "mype@test.com");

        verify(proyectoRepository, times(1)).save(any(Proyecto.class));
    }

    // ═══════════════════════════════════════════
    // PUBLICAR
    // ═══════════════════════════════════════════

    @Test
    void publicar_Success() {
        proyecto.setEstado(WorkflowEstado.BORRADOR);

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(estudianteRepository.findAll()).thenReturn(List.of(estudiante));

        proyectoService.publicar(1L, "mype@test.com");

        assertEquals(WorkflowEstado.PENDIENTE, proyecto.getEstado());
        verify(proyectoRepository, times(1)).save(proyecto);
    }

    @Test
    void publicar_ShouldFail_WhenNotBorrador() {
        proyecto.setEstado(WorkflowEstado.PENDIENTE);

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        assertThrows(BusinessException.class,
                () -> proyectoService.publicar(1L, "mype@test.com"));
    }

    // ═══════════════════════════════════════════
    // POSTULAR
    // ═══════════════════════════════════════════

    @Test
    void postular_Success() {
        proyecto.setEstado(WorkflowEstado.PENDIENTE);

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.existsPostulacionActiva(anyLong(), anyLong(), anyList())).thenReturn(false);
        when(postulacionRepository.save(any(Postulacion.class))).thenReturn(postulacion);

        PostulacionRequest postRequest = new PostulacionRequest("Quiero participar", null);
        PostulacionResponse response = proyectoService.postular(1L, postRequest, "est@test.com");

        assertNotNull(response);
        verify(postulacionRepository, times(1)).save(any(Postulacion.class));
    }

    @Test
    void postular_ShouldFail_WhenProjectNotPendiente() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        assertThrows(BusinessException.class,
                () -> proyectoService.postular(1L, new PostulacionRequest("Mensaje", null), "est@test.com"));
    }

    @Test
    void postular_ShouldFail_WhenAlreadyPostulado() {
        proyecto.setEstado(WorkflowEstado.PENDIENTE);

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.existsPostulacionActiva(anyLong(), anyLong(), anyList())).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> proyectoService.postular(1L, new PostulacionRequest("Mensaje", null), "est@test.com"));
    }

    // ═══════════════════════════════════════════
    // CANCELAR PROYECTO (admin)
    // ═══════════════════════════════════════════

    @Test
    void cancelarProyecto_Success() {
        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(usuarioAdmin));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(postulacionRepository.findByProyectoId(1L)).thenReturn(List.of(postulacion));

        proyectoService.cancelarProyecto(1L, "admin@test.com");

        assertEquals(WorkflowEstado.CANCELADO, proyecto.getEstado());
        assertEquals(EstadoPostulacion.RECHAZADO, postulacion.getEstado());
    }

    @Test
    void cancelarProyecto_ShouldFail_WhenCompletado() {
        proyecto.setEstado(WorkflowEstado.COMPLETADO);

        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(usuarioAdmin));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        assertThrows(BusinessException.class,
                () -> proyectoService.cancelarProyecto(1L, "admin@test.com"));
    }

    // ═══════════════════════════════════════════
    // EDITAR
    // ═══════════════════════════════════════════

    @Test
    void editar_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        EditarProyectoRequest request = new EditarProyectoRequest(
                "Nuevo Titulo", "Nueva desc", "Nuevo obj", "Nuevos req",
                "Nuevos entregables", AreaSistemas.DESARROLLO_MOVIL, 2,
                null, null, null);

        proyectoService.editar(1L, request, "mype@test.com");

        verify(proyectoRepository, times(1)).save(proyecto);
    }

    @Test
    void editar_ShouldFail_WhenEnDesarrollo() {
        proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        EditarProyectoRequest request = new EditarProyectoRequest(
                "Titulo", "Desc", null, null, null, null, null, null, null, null);

        assertThrows(BusinessException.class,
                () -> proyectoService.editar(1L, request, "mype@test.com"));
    }

    // ═══════════════════════════════════════════
    // ELIMINAR
    // ═══════════════════════════════════════════

    @Test
    void eliminar_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        proyectoService.eliminar(1L, "mype@test.com");

        verify(proyectoRepository, times(1)).delete(proyecto);
    }

    @Test
    void eliminar_ShouldFail_WhenEnDesarrollo() {
        proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        assertThrows(BusinessException.class,
                () -> proyectoService.eliminar(1L, "mype@test.com"));
    }

    // ═══════════════════════════════════════════
    // CERRAR PROYECTO
    // ═══════════════════════════════════════════

    @Test
    void cerrarProyecto_AsAdmin_Success() {
        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(usuarioAdmin));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        ProyectoResponse response = proyectoService.cerrarProyecto(1L, "admin@test.com");

        assertFalse(proyecto.getActivo());
    }

    @Test
    void cerrarProyecto_AsMype_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        ProyectoResponse response = proyectoService.cerrarProyecto(1L, "mype@test.com");

        assertFalse(proyecto.getActivo());
    }

    // ═══════════════════════════════════════════
    // CAMBIAR ESTADO POSTULACION
    // ═══════════════════════════════════════════

    @Test
    void cambiarEstadoPostulacion_AdminPreselecciona_Success() {
        proyecto.setEstado(WorkflowEstado.PENDIENTE);
        proyecto.setDelegarGestionAdmin(true);
        estudiante.setLimiteProyectos(3);

        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(usuarioAdmin));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findById(1L)).thenReturn(Optional.of(postulacion));
        when(postulacionRepository.findByProyectoId(1L)).thenReturn(List.of(postulacion));
        when(postulacionRepository.save(any(Postulacion.class))).thenReturn(postulacion);

        CambiarEstadoPostulacionRequest req = new CambiarEstadoPostulacionRequest(EstadoPostulacion.PRESELECCIONADO);
        PostulacionResponse response = proyectoService.cambiarEstadoPostulacion(1L, 1L, req, "admin@test.com");

        assertEquals(EstadoPostulacion.PRESELECCIONADO, postulacion.getEstado());
        verify(postulacionRepository, times(1)).save(postulacion);
    }

    @Test
    void cambiarEstadoPostulacion_MypeValida_Success() {
        postulacion.setEstado(EstadoPostulacion.PRESELECCIONADO);
        proyecto.setEstado(WorkflowEstado.PENDIENTE);
        estudiante.setLimiteProyectos(3);

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findById(1L)).thenReturn(Optional.of(postulacion));
        when(postulacionRepository.save(any(Postulacion.class))).thenReturn(postulacion);

        CambiarEstadoPostulacionRequest req = new CambiarEstadoPostulacionRequest(EstadoPostulacion.VALIDADO_MYPE);
        PostulacionResponse response = proyectoService.cambiarEstadoPostulacion(1L, 1L, req, "mype@test.com");

        assertEquals(EstadoPostulacion.VALIDADO_MYPE, postulacion.getEstado());
    }

    // ═══════════════════════════════════════════
    // CONFIRMAR POSTULACION
    // ═══════════════════════════════════════════

    @Test
    void confirmarPostulacion_Confirm_Success() {
        postulacion.setEstado(EstadoPostulacion.VALIDADO_MYPE);
        proyecto.setCupos(1);
        proyecto.setEstado(WorkflowEstado.PENDIENTE);

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(postulacionRepository.findById(1L)).thenReturn(Optional.of(postulacion));
        when(postulacionRepository.findByProyectoId(1L)).thenReturn(List.of(postulacion));
        when(postulacionRepository.save(any(Postulacion.class))).thenReturn(postulacion);
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);

        PostulacionResponse response = proyectoService.confirmarPostulacion(1L, true, "est@test.com");

        assertEquals(EstadoPostulacion.CONFIRMADO, postulacion.getEstado());
    }

    @Test
    void confirmarPostulacion_ShouldFail_WhenCupsFull() {
        postulacion.setEstado(EstadoPostulacion.VALIDADO_MYPE);
        proyecto.setCupos(1);

        Postulacion otroConfirmado = Postulacion.builder()
                .id(2L).proyecto(proyecto)
                .estado(EstadoPostulacion.CONFIRMADO)
                .build();

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(postulacionRepository.findById(1L)).thenReturn(Optional.of(postulacion));
        when(postulacionRepository.findByProyectoId(1L)).thenReturn(List.of(postulacion, otroConfirmado));

        assertThrows(BusinessException.class,
                () -> proyectoService.confirmarPostulacion(1L, true, "est@test.com"));
    }

    @Test
    void confirmarPostulacion_Reject_Success() {
        postulacion.setEstado(EstadoPostulacion.VALIDADO_MYPE);

        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(postulacionRepository.findById(1L)).thenReturn(Optional.of(postulacion));
        when(postulacionRepository.findByProyectoId(1L)).thenReturn(List.of(postulacion));
        when(postulacionRepository.save(any(Postulacion.class))).thenReturn(postulacion);

        PostulacionResponse response = proyectoService.confirmarPostulacion(1L, false, "est@test.com");

        assertEquals(EstadoPostulacion.RECHAZADO, postulacion.getEstado());
    }

    // ═══════════════════════════════════════════
    // COMPLETAR PROYECTO
    // ═══════════════════════════════════════════

    @Test
    void completarProyecto_Success() {
        proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(entregableRepository.findByProyectoIdWithDetails(1L)).thenReturn(List.of(entregable));
        when(proyectoRepository.save(any(Proyecto.class))).thenReturn(proyecto);
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of(postulacion));

        proyectoService.completarProyecto(1L, "mype@test.com");

        assertEquals(WorkflowEstado.COMPLETADO, proyecto.getEstado());
    }

    @Test
    void completarProyecto_ShouldFail_WhenNotEnDesarrollo() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        assertThrows(BusinessException.class,
                () -> proyectoService.completarProyecto(1L, "mype@test.com"));
    }

    @Test
    void completarProyecto_ShouldFail_WhenEntregablesPendientes() {
        proyecto.setEstado(WorkflowEstado.EN_DESARROLLO);
        entregable.setEstado(EstadoEntregable.PENDIENTE);

        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(entregableRepository.findByProyectoIdWithDetails(1L)).thenReturn(List.of(entregable));

        assertThrows(BusinessException.class,
                () -> proyectoService.completarProyecto(1L, "mype@test.com"));
    }

    // ═══════════════════════════════════════════
    // LISTAR
    // ═══════════════════════════════════════════

    @Test
    void listarPublicos_Success() {
        proyecto.setEstado(WorkflowEstado.PENDIENTE);
        when(proyectoRepository.findPublicosConMype(WorkflowEstado.PENDIENTE)).thenReturn(List.of(proyecto));

        var result = proyectoService.listarPublicos(org.springframework.data.domain.PageRequest.of(0, 10));

        assertFalse(result.isEmpty());
    }

    @Test
    void obtenerPorId_Success() {
        when(proyectoRepository.findByIdWithMype(1L)).thenReturn(Optional.of(proyecto));

        ProyectoResponse response = proyectoService.obtenerPorId(1L);

        assertNotNull(response);
    }

    @Test
    void listarPostulaciones_Success() {
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(mypeRepository.findByUsuarioId(1L)).thenReturn(Optional.of(mype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findByProyectoIdWithDetails(1L)).thenReturn(List.of(postulacion));
        when(postulacionRepository.countByEstudianteIdAndEstadoAndProyectoEstadoIn(anyLong(), any(), anyList())).thenReturn(0L);

        List<PostulacionResponse> responses = proyectoService.listarPostulaciones(1L, "mype@test.com");

        assertFalse(responses.isEmpty());
    }

    @Test
    void misPostulaciones_Success() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(postulacionRepository.findByEstudianteIdWithDetails(1L)).thenReturn(List.of(postulacion));
        when(postulacionRepository.findByProyectoIdAndEstadoWithDetails(1L, EstadoPostulacion.CONFIRMADO))
                .thenReturn(List.of());

        List<PostulacionResponse> responses = proyectoService.misPostulaciones("est@test.com");

        assertFalse(responses.isEmpty());
    }
}
