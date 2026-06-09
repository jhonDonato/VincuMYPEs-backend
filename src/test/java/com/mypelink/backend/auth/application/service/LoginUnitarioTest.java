package com.mypelink.backend.auth.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.auth.recovery.domain.repository.PasswordResetRepository;
import com.mypelink.backend.shared.application.service.ConfiguracionService;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.shared.infrastructure.websocket.WebSocketNotificationService;
import com.mypelink.backend.usuarios.application.dto.AuthResponse;
import com.mypelink.backend.usuarios.application.dto.LoginRequest;
import com.mypelink.backend.usuarios.application.service.AuthService;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUnitarioTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private MypeRepository mypeRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private WebSocketNotificationService webSocketNotificationService;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordResetRepository passwordResetRepository;
    @Mock private EmailService emailService;
    @Mock private ConfiguracionService configuracionService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "verificationEnabled", true);
        ReflectionTestUtils.setField(authService, "accessExpiration", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshExpiration", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshExpirationRemember", 604800000L);
        ReflectionTestUtils.setField(authService, "maxAttempts", 5);
        ReflectionTestUtils.setField(authService, "blockDurationMinutes", 10L);
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("N00123456@upn.pe", "Juan@1234", false);

        when(loginAttemptRepository.findFirstByEmailOrderByIdDesc(request.email())).thenReturn(Optional.empty());
        
        Authentication mockAuth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);

        Role roleEstudiante = new Role();
        roleEstudiante.setNombre("ROLE_ESTUDIANTE");
        
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("N00123456@upn.pe")
                .password("encodedPass")
                .rol(roleEstudiante)
                .activo(true)
                .build();
                
        when(usuarioRepository.findByEmailWithRole(request.email())).thenReturn(Optional.of(usuario));
        when(configuracionService.isModoMantenimiento()).thenReturn(false);
        when(jwtService.generateToken(any(), any(), anyLong())).thenReturn("mockJwtToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.token());
        assertEquals("ESTUDIANTE", response.rol());
        verify(loginAttemptRepository, times(1)).deleteByEmail(request.email());
        verify(refreshTokenRepository, times(1)).save(any());
    }
}
