package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.auth.recovery.domain.model.PasswordReset;
import com.mypelink.backend.auth.recovery.domain.repository.PasswordResetRepository;
import com.mypelink.backend.shared.application.service.ConfiguracionService;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.shared.infrastructure.websocket.WebSocketNotificationService;
import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.domain.model.*;
import com.mypelink.backend.usuarios.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    private Role roleEst;
    private Role roleMype;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        roleEst = Role.builder().id(1L).nombre("ROLE_ESTUDIANTE").build();
        roleMype = Role.builder().id(2L).nombre("ROLE_MYPE").build();
        usuario = Usuario.builder()
                .id(1L).nombre("Juan").email("juan@upn.pe")
                .password("encoded").rol(roleEst).activo(true).build();

        ReflectionTestUtils.setField(authService, "verificationEnabled", false);
        ReflectionTestUtils.setField(authService, "accessExpiration", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshExpiration", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshExpirationRemember", 604800000L);
        ReflectionTestUtils.setField(authService, "maxAttempts", 5);
        ReflectionTestUtils.setField(authService, "blockDurationMinutes", 10L);
    }

    @Test
    void registerEstudiante_Success() {
        when(configuracionService.isModoMantenimiento()).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan@upn.pe")).thenReturn(false);
        when(usuarioRepository.existsByTelefono("999888777")).thenReturn(false);
        when(roleRepository.findByNombre("ROLE_ESTUDIANTE")).thenReturn(Optional.of(roleEst));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(null);
        when(jwtService.generateToken(any(), anyMap(), anyLong())).thenReturn("token");

        AuthResponse response = authService.registerEstudiante(
                new RegisterEstudianteRequest("12345678", "Juan", "juan@upn.pe",
                        "Pass1234!", "999888777", "N00123456", null, null));

        assertNotNull(response);
        assertEquals("token", response.token());
    }

    @Test
    void registerEstudiante_ShouldFail_WhenEmailExists() {
        when(configuracionService.isModoMantenimiento()).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan@upn.pe")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> authService.registerEstudiante(
                        new RegisterEstudianteRequest("12345678", "Juan", "juan@upn.pe",
                                "Pass1234!", "999888777", "N00123456", null, null)));
    }

    @Test
    void registerMype_Success() {
        when(configuracionService.isModoMantenimiento()).thenReturn(false);
        when(usuarioRepository.existsByEmail("mype@test.com")).thenReturn(false);
        when(usuarioRepository.existsByTelefono("999888777")).thenReturn(false);
        when(mypeRepository.existsByRuc("10123456789")).thenReturn(false);
        when(roleRepository.findByNombre("ROLE_MYPE")).thenReturn(Optional.of(roleMype));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(
                Usuario.builder().id(2L).nombre("Mype").email("mype@test.com")
                        .password("encoded").rol(roleMype).build());
        when(mypeRepository.save(any(Mype.class))).thenReturn(null);
        when(jwtService.generateToken(any(), anyMap(), anyLong())).thenReturn("token");

        AuthResponse response = authService.registerMype(
                new RegisterMypeRequest("Mype", "mype@test.com", "Pass1234!",
                        "999888777", "MYPE SAC", "MYPE SAC", "10123456789", "Tec", "Av Test"));

        assertNotNull(response);
        assertEquals("token", response.token());
    }

    @Test
    void login_Success() {
        when(loginAttemptRepository.findFirstByEmailOrderByIdDesc("juan@upn.pe")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(usuarioRepository.findByEmailWithRole("juan@upn.pe")).thenReturn(Optional.of(usuario));
        when(configuracionService.isModoMantenimiento()).thenReturn(false);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(null);
        when(jwtService.generateToken(any(), anyMap(), anyLong())).thenReturn("access-token");

        AuthResponse response = authService.login(new LoginRequest("juan@upn.pe", "Pass1234!", false));

        assertNotNull(response);
        assertEquals("access-token", response.token());
        assertNotNull(response.refreshToken());
    }

    @Test
    void login_ShouldFail_WhenInvalidCredentials() {
        when(loginAttemptRepository.findFirstByEmailOrderByIdDesc("juan@upn.pe")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));
        when(loginAttemptRepository.countByEmailAndAttemptTimeAfter(anyString(), any())).thenReturn(0L);
        when(loginAttemptRepository.save(any(LoginAttempt.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("juan@upn.pe", "wrong", false)));
    }

    @Test
    void logout_Success() {
        RefreshToken rt = RefreshToken.builder().token("rt").revoked(false).build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("rt")).thenReturn(Optional.of(rt));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(rt);

        authService.logout("rt");

        assertTrue(rt.isRevoked());
    }

    @Test
    void refreshAccessToken_Success() {
        RefreshToken rt = RefreshToken.builder()
                .token("rt").userId(1L).revoked(false)
                .expiryDate(LocalDateTime.now().plusHours(1)).build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("rt")).thenReturn(Optional.of(rt));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any(), anyMap(), anyLong())).thenReturn("new-access-token");

        AuthResponse response = authService.refreshAccessToken("rt");

        assertEquals("new-access-token", response.token());
    }

    @Test
    void refreshAccessToken_ShouldFail_WhenExpired() {
        RefreshToken rt = RefreshToken.builder()
                .token("rt").userId(1L).revoked(false)
                .expiryDate(LocalDateTime.now().minusHours(1)).build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("rt")).thenReturn(Optional.of(rt));

        assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken("rt"));
    }

    @Test
    void changePassword_Success() {
        when(usuarioRepository.findByEmailWithRole("juan@upn.pe")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("oldPass", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPass1!")).thenReturn("newEncoded");

        authService.changePassword("juan@upn.pe", new ChangePasswordRequest("oldPass", "newPass1!"));

        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void sendVerificationOtp_Success() {
        ReflectionTestUtils.setField(authService, "verificationEnabled", true);
        when(configuracionService.isModoMantenimiento()).thenReturn(false);
        when(passwordResetRepository.save(any(PasswordReset.class))).thenReturn(null);

        authService.sendVerificationOtp("juan@upn.pe");

        verify(emailService, times(1)).sendOtpEmail(anyString(), anyString(), anyString());
    }

    @Test
    void verifyOtp_Success() {
        ReflectionTestUtils.setField(authService, "verificationEnabled", true);
        when(configuracionService.isModoMantenimiento()).thenReturn(false);
        PasswordReset reset = PasswordReset.builder().build();
        when(passwordResetRepository.findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(
                anyString(), anyString(), any())).thenReturn(Optional.of(reset));

        assertTrue(authService.verifyOtp("juan@upn.pe", "123456"));
    }
}
