package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.application.dto.ActualizarLimiteProyectosRequest;
import com.mypelink.backend.usuarios.application.dto.EstudianteAdminResponse;
import com.mypelink.backend.usuarios.application.service.AdminEstudianteService;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminEstudianteServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AdminEstudianteService adminEstudianteService;

    private Usuario admin;
    private Usuario estUser;
    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        admin = Usuario.builder().id(1L).nombre("Admin").email("admin@test.com")
                .rol(Role.builder().nombre("ROLE_ADMIN").build()).build();
        estUser = Usuario.builder().id(2L).nombre("Est").email("est@test.com")
                .rol(Role.builder().nombre("ROLE_ESTUDIANTE").build()).build();
        estudiante = Estudiante.builder().id(1L).usuario(estUser).limiteProyectos(3).activo(true).build();
    }

    @Test
    void listarTodos_Success() {
        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(admin));
        when(estudianteRepository.findAll()).thenReturn(List.of(estudiante));

        List<EstudianteAdminResponse> result = adminEstudianteService.listarTodos("admin@test.com");

        assertFalse(result.isEmpty());
        assertEquals("Est", result.get(0).nombre());
    }

    @Test
    void actualizarLimiteProyectos_Success() {
        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(admin));
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudiante);

        EstudianteAdminResponse response = adminEstudianteService.actualizarLimiteProyectos(
                1L, new ActualizarLimiteProyectosRequest(2), "admin@test.com");

        assertEquals(2, response.limiteProyectos());
    }

    @Test
    void actualizarLimiteProyectos_ShouldFail_WhenNotAdmin() {
        when(usuarioRepository.findByEmailWithRole("est@test.com")).thenReturn(Optional.of(estUser));

        assertThrows(BusinessException.class,
                () -> adminEstudianteService.actualizarLimiteProyectos(
                        1L, new ActualizarLimiteProyectosRequest(2), "est@test.com"));
    }

    @Test
    void actualizarLimitePorUsuarioId_Success() {
        when(usuarioRepository.findByEmailWithRole("admin@test.com")).thenReturn(Optional.of(admin));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudiante);

        EstudianteAdminResponse response = adminEstudianteService.actualizarLimitePorUsuarioId(
                2L, new ActualizarLimiteProyectosRequest(2), "admin@test.com");

        assertEquals(2, response.limiteProyectos());
    }
}
