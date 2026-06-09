package com.mypelink.backend.auth.application.service;

import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.application.dto.ChangePasswordRequest;
import com.mypelink.backend.usuarios.application.service.AuthService;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.RefreshTokenRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContrasenaUnitarioTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass@123", "NewPass@123");
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPassword("encodedOldPass");

        when(usuarioRepository.findByEmailWithRole("user@upn.pe")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.oldPassword(), usuario.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.newPassword())).thenReturn("encodedNewPass");

        authService.changePassword("user@upn.pe", request);

        verify(usuarioRepository, times(1)).save(usuario);
        verify(refreshTokenRepository, times(1)).deleteAllByUserId(1L);
    }

    @Test
    void changePassword_WrongOldPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass@123", "NewPass@123");
        Usuario usuario = new Usuario();
        usuario.setPassword("encodedOldPass");

        when(usuarioRepository.findByEmailWithRole("user@upn.pe")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(request.oldPassword(), usuario.getPassword())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.changePassword("user@upn.pe", request);
        });

        assertTrue(exception.getMessage().contains("La contraseña actual es incorrecta"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
}
