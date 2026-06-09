package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.application.service.UsuarioService;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private Role role;

    @BeforeEach
    void setUp() {
        role = Role.builder().id(1L).nombre("ROLE_ESTUDIANTE").build();
        usuario = Usuario.builder()
                .id(1L).nombre("Juan").email("juan@test.com")
                .password("encodedPass").rol(role).activo(true).build();
    }

    @Test
    void actualizarInfo_Success() {
        when(usuarioRepository.findByEmailWithRole("juan@test.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        UsuarioResponse response = usuarioService.actualizarInfo(
                "juan@test.com", new ActualizarInfoRequest("Juan Perez", "999888777"));

        assertEquals("Juan Perez", response.nombre());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void cambiarPassword_Success() {
        when(usuarioRepository.findByEmailWithRole("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("oldPass", "encodedPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newEncoded");

        usuarioService.cambiarPassword("juan@test.com",
                new CambiarPasswordRequest("oldPass", "newPass123"));

        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void cambiarPassword_ShouldFail_WhenWrongPassword() {
        when(usuarioRepository.findByEmailWithRole("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> usuarioService.cambiarPassword("juan@test.com",
                        new CambiarPasswordRequest("wrong", "newPass123")));
    }

    @Test
    void cambiarEmail_Success() {
        when(usuarioRepository.findByEmailWithRole("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("pass", "encodedPass")).thenReturn(true);
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(jwtService.generateToken(any(), anyMap())).thenReturn("newToken");

        CambiarEmailResponse response = usuarioService.cambiarEmail("juan@test.com",
                new CambiarEmailRequest("nuevo@test.com", "pass"));

        assertEquals("newToken", response.nuevoToken());
        assertEquals("nuevo@test.com", usuario.getEmail());
    }

    @Test
    void desactivarCuenta_Success() {
        when(usuarioRepository.findByEmailWithRole("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("pass", "encodedPass")).thenReturn(true);

        usuarioService.desactivarCuenta("juan@test.com", new ConfirmarPasswordRequest("pass"));

        assertFalse(usuario.getActivo());
        verify(usuarioRepository, times(1)).save(usuario);
    }
}
