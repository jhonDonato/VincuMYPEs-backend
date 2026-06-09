package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.ejecucion.domain.repository.EvaluacionRepository;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.application.service.AdminUsuarioService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private MypeRepository mypeRepository;
    @Mock private ProyectoRepository proyectoRepository;
    @Mock private PostulacionRepository postulacionRepository;
    @Mock private EvaluacionRepository evaluacionRepository;

    @InjectMocks
    private AdminUsuarioService adminUsuarioService;

    private Role roleAdmin;
    private Role roleEst;
    private Role roleMype;
    private Usuario admin;
    private Usuario estudianteUser;
    private Usuario mypeUser;
    private Estudiante estudiante;
    private Mype mype;

    @BeforeEach
    void setUp() {
        roleAdmin = Role.builder().id(1L).nombre("ROLE_ADMIN").build();
        roleEst = Role.builder().id(2L).nombre("ROLE_ESTUDIANTE").build();
        roleMype = Role.builder().id(3L).nombre("ROLE_MYPE").build();

        admin = Usuario.builder().id(1L).nombre("Admin").email("admin@test.com").rol(roleAdmin).activo(true).build();
        estudianteUser = Usuario.builder().id(2L).nombre("Est").email("est@test.com").rol(roleEst).activo(true).build();
        mypeUser = Usuario.builder().id(3L).nombre("Mype").email("mype@test.com").rol(roleMype).activo(true).build();

        estudiante = Estudiante.builder().id(1L).usuario(estudianteUser).carrera("Ing Sistemas").limiteProyectos(3).activo(true).build();
        mype = Mype.builder().id(1L).usuario(mypeUser).rubro("Tecnologia").activo(true).build();
    }

    @Test
    void listarUsuarios_AllRoles_Success() {
        when(usuarioRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(admin, estudianteUser, mypeUser)));

        Page<?> result = adminUsuarioService.listarUsuarios(PageRequest.of(0, 10), null);

        assertEquals(3, result.getTotalElements());
    }

    @Test
    void cambiarEstadoUsuario_ToggleActive_Success() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(estudianteUser));
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));

        adminUsuarioService.cambiarEstadoUsuario(2L);

        assertFalse(estudianteUser.getActivo());
        assertFalse(estudiante.getActivo());
    }

    @Test
    void cambiarEstadoUsuario_ShouldFail_WhenAdmin() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(BusinessException.class,
                () -> adminUsuarioService.cambiarEstadoUsuario(1L));
    }

    @Test
    void cambiarBypassLimite_Success() {
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));

        adminUsuarioService.cambiarBypassLimite(2L, 1);

        assertEquals(1, estudiante.getLimiteProyectos().intValue());
    }

    @Test
    void cambiarBypassLimite_WithHigherLimit_ValidatesReputation() {
        estudiante.setLimiteProyectos(1);
        when(estudianteRepository.findByUsuarioId(2L)).thenReturn(Optional.of(estudiante));
        when(postulacionRepository.findByEstudianteIdWithDetails(1L)).thenReturn(List.of());
        when(evaluacionRepository.findByEstudianteId(1L)).thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> adminUsuarioService.cambiarBypassLimite(2L, 2));
    }

    @Test
    void cambiarBypassLimite_ShouldFail_WhenInvalidLimit() {
        assertThrows(BusinessException.class,
                () -> adminUsuarioService.cambiarBypassLimite(2L, 0));
    }
}
