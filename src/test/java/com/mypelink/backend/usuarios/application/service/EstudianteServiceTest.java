package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.application.dto.EstudianteProfileResponse;
import com.mypelink.backend.usuarios.application.dto.EstudiantePublicoResponse;
import com.mypelink.backend.usuarios.application.dto.UpdateEstudianteRequest;
import com.mypelink.backend.usuarios.application.service.EstudianteService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private S3Service s3Service;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private MypeRepository mypeRepository;

    @InjectMocks
    private EstudianteService estudianteService;

    private Role roleEst;
    private Role roleAdmin;
    private Role roleMype;
    private Usuario usuarioEst;
    private Usuario usuarioAdmin;
    private Usuario usuarioMypeUser;
    private Estudiante estudiante;
    private Mype mype;

    @BeforeEach
    void setUp() {
        roleEst = Role.builder().id(1L).nombre("ROLE_ESTUDIANTE").build();
        roleAdmin = Role.builder().id(2L).nombre("ROLE_ADMIN").build();
        roleMype = Role.builder().id(3L).nombre("ROLE_MYPE").build();

        usuarioEst = Usuario.builder().id(1L).nombre("Est").email("est@test.com").rol(roleEst).build();
        usuarioAdmin = Usuario.builder().id(2L).nombre("Admin").email("admin@test.com").rol(roleAdmin).build();
        usuarioMypeUser = Usuario.builder().id(3L).nombre("Mype").email("mype@test.com").rol(roleMype).build();

        estudiante = Estudiante.builder()
                .id(1L).usuario(usuarioEst).universidad("UPN")
                .carrera("Ing Sistemas").limiteProyectos(3)
                .build();

        mype = Mype.builder().id(1L).usuario(usuarioMypeUser).nombreComercial("MYPE SAS").build();
    }

    @Test
    void getProfile_Success() {
        when(usuarioRepository.findByEmail("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));

        EstudianteProfileResponse response = estudianteService.getProfile("est@test.com");

        assertNotNull(response);
        assertEquals("UPN", response.universidad());
    }

    @Test
    void updateProfile_Success() {
        when(usuarioRepository.findByEmail("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudiante);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEst);

        UpdateEstudianteRequest request = new UpdateEstudianteRequest(
                "Bio nueva", "Java, Spring", "http://portafolio.com",
                "http://linkedin.com", "Ing Software", "UPN",
                null, null, null, null, null, null, null);

        EstudianteProfileResponse response = estudianteService.updateProfile("est@test.com", request);

        assertEquals("Bio nueva", estudiante.getBio());
        verify(estudianteRepository, times(1)).save(estudiante);
    }

    @Test
    void subirCv_Success() {
        when(usuarioRepository.findByEmail("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));
        when(s3Service.subirCvPdf(any())).thenReturn("https://s3.test/cv.pdf");
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudiante);

        EstudianteProfileResponse response = estudianteService.subirCv("est@test.com", null);

        assertEquals("https://s3.test/cv.pdf", estudiante.getCvUrl());
    }

    @Test
    void obtenerPerfilPublico_AsAdmin_RevelaSensibles() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(usuarioAdmin));

        EstudiantePublicoResponse response = estudianteService.obtenerPerfilPublico(1L, "admin@test.com");

        assertNotNull(response);
        assertEquals("est@test.com", response.email());
    }

    @Test
    void obtenerPerfilPublico_AsAdmin_ReturnsNonNullFields() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(usuarioAdmin));

        EstudiantePublicoResponse response = estudianteService.obtenerPerfilPublico(1L, "admin@test.com");

        assertNotNull(response.usuarioId());
    }

    @Test
    void obtenerPerfilPublico_AsMype_Success() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMypeUser));
        when(mypeRepository.findByUsuarioId(3L)).thenReturn(Optional.of(mype));
        when(postulacionRepository.existsPostulacionDeEstudianteEnProyectoDeMype(1L, 1L)).thenReturn(true);

        EstudiantePublicoResponse response = estudianteService.obtenerPerfilPublico(1L, "mype@test.com");

        assertNotNull(response);
        assertNull(response.email());
    }

    @Test
    void obtenerPerfilPublico_AsMype_ShouldFail_WhenNoRelation() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findByEmailWithRole("mype@test.com")).thenReturn(Optional.of(usuarioMypeUser));
        when(mypeRepository.findByUsuarioId(3L)).thenReturn(Optional.of(mype));
        when(postulacionRepository.existsPostulacionDeEstudianteEnProyectoDeMype(1L, 1L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> estudianteService.obtenerPerfilPublico(1L, "mype@test.com"));
    }

    @Test
    void obtenerPerfilPublico_AsOwnStudent_Success() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(usuarioEst));
        when(estudianteRepository.findByUsuarioId(1L)).thenReturn(Optional.of(estudiante));

        EstudiantePublicoResponse response = estudianteService.obtenerPerfilPublico(1L, "est@test.com");

        assertNotNull(response);
    }
}
