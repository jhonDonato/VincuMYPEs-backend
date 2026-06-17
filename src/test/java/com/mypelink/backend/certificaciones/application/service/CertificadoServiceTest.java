package com.mypelink.backend.certificaciones.application.service;

import com.mypelink.backend.certificaciones.application.dto.CertificadoResponse;
import com.mypelink.backend.certificaciones.application.dto.EmitirCertificadoRequest;
import com.mypelink.backend.certificaciones.application.service.CertificadoService;
import com.mypelink.backend.certificaciones.application.service.PdfGeneratorService;
import com.mypelink.backend.certificaciones.domain.model.Certificado;
import com.mypelink.backend.certificaciones.domain.repository.CertificadoRepository;
import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificadoServiceTest {

    @Mock private CertificadoRepository certificadoRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;
    @Mock private PdfGeneratorService pdfGeneratorService;
    @Mock private S3Service s3Service;

    @InjectMocks
    private CertificadoService certificadoService;

    private Usuario usuarioMype;
    private Usuario usuarioAdmin;
    private Mype mype;
    private Proyecto proyecto;
    private Postulacion postulacion;
    private Certificado certificado;
    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        Role roleAdmin = Role.builder().id(1L).nombre("ROLE_ADMIN").build();
        Role roleMype = Role.builder().id(2L).nombre("ROLE_MYPE").build();

        usuarioAdmin = Usuario.builder().id(99L).nombre("Admin").email("admin@test.com").rol(roleAdmin).build();
        usuarioMype = Usuario.builder().id(1L).nombre("MYPE Test").email("mype@test.com").rol(roleMype).build();

        Usuario usuarioEst = Usuario.builder().id(3L).nombre("Estudiante").email("est@test.com").build();
        estudiante = Estudiante.builder().id(1L).usuario(usuarioEst).build();

        mype = Mype.builder().id(1L).usuario(usuarioMype).nombreComercial("MYPE Test SAS").build();

        proyecto = Proyecto.builder()
                .id(1L).titulo("Proyecto Test").mype(mype)
                .estado(WorkflowEstado.COMPLETADO).build();

        postulacion = Postulacion.builder()
                .id(1L).proyecto(proyecto).estudiante(estudiante)
                .estado(EstadoPostulacion.CONFIRMADO)
                .build();

        certificado = Certificado.builder()
                .id(1L).codigo("CERT-ABC123").proyecto(proyecto)
                .estudiante(estudiante)
                .tituloCertificado("Certificado Test")
                .urlCertificado("https://s3.test/cert.pdf")
                .emitidoPor(usuarioMype)
                .fechaEmision(LocalDate.now())
                .build();
    }

    @Test
    void emitirCertificados_Success() {
        when(usuarioRepository.findByEmail("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(postulacion));
        when(certificadoRepository.existsByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(false);
        when(certificadoRepository.save(any(Certificado.class))).thenReturn(certificado);
        when(certificadoRepository.findById(1L)).thenReturn(Optional.of(certificado));
        when(pdfGeneratorService.generarCertificadoPDF(any(), any(), any(), any(), any())).thenReturn(new byte[]{1,2,3});
        when(s3Service.subirCertificado(any(), any())).thenReturn("https://s3.test/cert.pdf");

        EmitirCertificadoRequest request = // ✅ CORRECTO: Debe tener 9 parámetros
                new EmitirCertificadoRequest(
                        1L,                    // proyectoId
                        List.of(1L),          // estudiantesIds
                        "Certificado Test",   // tituloCertificado
                        "Desc",               // descripcionCertificado
                        null,                 // firmaBase64
                        null,                 // gerenteNombre
                        null,                 // proyectoTitulo (NUEVO)
                        null,                 // estudianteNombre (NUEVO)
                        null                  // mypeNombre (NUEVO)
                );
        List<CertificadoResponse> responses = certificadoService.emitirCertificados("mype@test.com", request);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(certificadoRepository, times(2)).save(any(Certificado.class));
    }

    @Test
    void emitirCertificados_ShouldFail_WhenNotMypeOwner() {
        when(usuarioRepository.findByEmail("mype@test.com")).thenReturn(Optional.of(usuarioAdmin));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));

        EmitirCertificadoRequest request = // ✅ CORRECTO: Debe tener 9 parámetros
                new EmitirCertificadoRequest(
                        1L,                    // proyectoId
                        List.of(1L),          // estudiantesIds
                        "Certificado Test",   // tituloCertificado
                        "Desc",               // descripcionCertificado
                        null,                 // firmaBase64
                        null,                 // gerenteNombre
                        null,                 // proyectoTitulo (NUEVO)
                        null,                 // estudianteNombre (NUEVO)
                        null                  // mypeNombre (NUEVO)
                );
        assertThrows(BusinessException.class,
                () -> certificadoService.emitirCertificados("mype@test.com", request));
    }

    @Test
    void emitirCertificados_ShouldFail_WhenProjectNotCompleted() {
        Proyecto proyectoPendiente = Proyecto.builder()
                .id(2L).titulo("Proyecto Pendiente").mype(mype)
                .estado(WorkflowEstado.PENDIENTE).build();

        when(usuarioRepository.findByEmail("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(proyectoRepository.findById(2L)).thenReturn(Optional.of(proyectoPendiente));

        EmitirCertificadoRequest request = // ✅ CORRECTO: Debe tener 9 parámetros
                new EmitirCertificadoRequest(
                        1L,                    // proyectoId
                        List.of(1L),          // estudiantesIds
                        "Certificado Test",   // tituloCertificado
                        "Desc",               // descripcionCertificado
                        null,                 // firmaBase64
                        null,                 // gerenteNombre
                        null,                 // proyectoTitulo (NUEVO)
                        null,                 // estudianteNombre (NUEVO)
                        null                  // mypeNombre (NUEVO)
                );
        assertThrows(BusinessException.class,
                () -> certificadoService.emitirCertificados("mype@test.com", request));
    }

    @Test
    void emitirCertificados_ShouldSkip_WhenCertAlreadyExists() {
        when(usuarioRepository.findByEmail("mype@test.com")).thenReturn(Optional.of(usuarioMype));
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyecto));
        when(postulacionRepository.findByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(Optional.of(postulacion));
        when(certificadoRepository.existsByProyectoIdAndEstudianteId(1L, 1L)).thenReturn(true);

        EmitirCertificadoRequest request = // ✅ CORRECTO: Debe tener 9 parámetros
                new EmitirCertificadoRequest(
                        1L,                    // proyectoId
                        List.of(1L),          // estudiantesIds
                        "Certificado Test",   // tituloCertificado
                        "Desc",               // descripcionCertificado
                        null,                 // firmaBase64
                        null,                 // gerenteNombre
                        null,                 // proyectoTitulo (NUEVO)
                        null,                 // estudianteNombre (NUEVO)
                        null                  // mypeNombre (NUEVO)
                );
        List<CertificadoResponse> responses = certificadoService.emitirCertificados("mype@test.com", request);

        assertTrue(responses.isEmpty());
        verify(certificadoRepository, never()).save(any(Certificado.class));
    }

    @Test
    void enviarCertificado_Success() {
        when(certificadoRepository.findById(1L)).thenReturn(Optional.of(certificado));
        doNothing().when(emailService).enviarCertificado(any(), any(), any(), any(), any(), any());

        certificadoService.enviarCertificado(1L, "mype@test.com");

        assertNotNull(certificado.getFechaEnvio());
        verify(emailService, times(1)).enviarCertificado(any(), any(), any(), any(), any(), any());
    }

    @Test
    void enviarCertificado_ShouldFail_WhenAlreadySent() {
        certificado.setFechaEnvio(LocalDateTime.now());
        when(certificadoRepository.findById(1L)).thenReturn(Optional.of(certificado));

        assertThrows(BusinessException.class,
                () -> certificadoService.enviarCertificado(1L, "mype@test.com"));
    }

    @Test
    void enviarCertificado_ShouldFail_WhenNotEmitter() {
        when(certificadoRepository.findById(1L)).thenReturn(Optional.of(certificado));

        assertThrows(BusinessException.class,
                () -> certificadoService.enviarCertificado(1L, "otro@test.com"));
    }

    @Test
    void eliminarCertificado_Success() {
        when(certificadoRepository.findById(1L)).thenReturn(Optional.of(certificado));
        doNothing().when(s3Service).eliminarArchivo(any());

        certificadoService.eliminarCertificado(1L, "mype@test.com");

        verify(certificadoRepository, times(1)).delete(certificado);
        verify(s3Service, times(1)).eliminarArchivo(any());
    }

    @Test
    void eliminarCertificado_ShouldFail_WhenAlreadySent() {
        certificado.setFechaEnvio(LocalDateTime.now());
        when(certificadoRepository.findById(1L)).thenReturn(Optional.of(certificado));

        assertThrows(BusinessException.class,
                () -> certificadoService.eliminarCertificado(1L, "mype@test.com"));
    }

    @Test
    void listarMisCertificados_Success() {
        when(certificadoRepository.findByEstudianteUsuarioEmailEager("est@test.com"))
                .thenReturn(List.of(certificado));

        List<CertificadoResponse> result = certificadoService.listarMisCertificados("est@test.com");

        assertEquals(1, result.size());
    }

    @Test
    void listarCertificadosEmitidos_Success() {
        when(certificadoRepository.findByProyectoMypeUsuarioEmailEager("mype@test.com"))
                .thenReturn(List.of(certificado));

        List<CertificadoResponse> result = certificadoService.listarCertificadosEmitidos("mype@test.com");

        assertEquals(1, result.size());
    }

    @Test
    void listarTodosCertificados_Success() {
        when(certificadoRepository.findAll()).thenReturn(List.of(certificado));

        var result = certificadoService.listarTodosCertificados();

        assertEquals(1, result.size());
    }
}
