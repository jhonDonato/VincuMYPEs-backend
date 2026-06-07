package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.auth.recovery.domain.model.PasswordReset;
import com.mypelink.backend.auth.recovery.domain.repository.PasswordResetRepository;
import com.mypelink.backend.shared.application.service.ConfiguracionService;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.domain.model.*;
import com.mypelink.backend.usuarios.domain.repository.*;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${app.email.verification.enabled:false}")
    private boolean verificationEnabled;

    @Value("${app.jwt.access-expiration:3600000}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-expiration:3600000}")
    private long refreshExpiration;

    @Value("${app.jwt.refresh-expiration-remember:604800000}")
    private long refreshExpirationRemember;

    @Value("${app.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.login.block-duration-minutes:10}")
    private long blockDurationMinutes;

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final MypeRepository mypeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.mypelink.backend.shared.infrastructure.websocket.WebSocketNotificationService webSocketNotificationService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetRepository passwordResetRepository;
    private final EmailService emailService;
    private final ConfiguracionService configuracionService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8 ||
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*\\d.*") ||
                !password.matches(".*[@$!%*?&\\-_#].*")) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un símbolo (@$!%*?&\\-_#)");
        }
    }


    @Transactional
    public AuthResponse registerEstudiante(RegisterEstudianteRequest request) {
        if (configuracionService.isModoMantenimiento()) {
            throw new BusinessException(
                    "Sistema en mantenimiento. Solo administradores pueden ingresar.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (verificationEnabled) {
            if (!request.email().toLowerCase().endsWith("@upn.pe")) {
                throw new BusinessException("Solo se permiten correos institucionales @upn.pe");
            }
            if (request.codigoEstudiante() == null || request.codigoEstudiante().isBlank()) {
                throw new BusinessException("El código de estudiante es obligatorio");
            }
        }
        if (request.codigoEstudiante() != null && !request.codigoEstudiante().matches("^N00\\d{6}$")) {
            throw new BusinessException("El código de estudiante debe tener el formato N00XXXXXX");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo electrónico ya está registrado en otra cuenta.");
        }
        if (request.dni() == null || request.dni().isBlank()) {
            throw new BusinessException("El DNI es obligatorio");
        }
        if (request.dni() != null && !request.dni().matches("\\d{8}")) {
            throw new BusinessException("El DNI debe tener 8 dígitos");
        }
        if (request.codigoEstudiante() != null &&
                estudianteRepository.existsByCodigoEstudiante(request.codigoEstudiante())) {
            throw new BusinessException("Este código de estudiante ya ha sido utilizado.");
        }
        if (request.telefono() == null || !request.telefono().matches("\\d{9}")) {
            throw new BusinessException("El teléfono debe tener 9 dígitos");
        }
        if (usuarioRepository.existsByTelefono(request.telefono())) {
            throw new BusinessException("El número de teléfono ya está registrado");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("La contraseña es obligatoria");
        }
        validatePasswordStrength(request.password());

        Role role = roleRepository.findByNombre("ROLE_ESTUDIANTE")
                .orElseThrow(() -> new BusinessException(
                        "Rol ROLE_ESTUDIANTE no encontrado. Contacte al administrador."));

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .dni(request.dni())
                .password(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .rol(role)
                .build());

        estudianteRepository.save(Estudiante.builder()
                .usuario(usuario)
                .codigoEstudiante(request.codigoEstudiante())
                .carrera(request.carrera() != null
                        ? request.carrera() : "Ingeniería de Sistemas Computacionales")
                .universidad(request.universidad() != null
                        ? request.universidad() : "Universidad Privada del Norte")
                .build());

        return buildAuthResponseSinRefresh(usuario, role.getNombre());
    }


    @Transactional
    public AuthResponse registerMype(RegisterMypeRequest request) {
        if (configuracionService.isModoMantenimiento()) {
            throw new BusinessException(
                    "Sistema en mantenimiento. Solo administradores pueden ingresar.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo electrónico ya está registrado en otra cuenta.");
        }
        if (request.ruc() != null && mypeRepository.existsByRuc(request.ruc())) {
            throw new BusinessException("Este RUC ya se encuentra registrado.");

        }
        if (request.ruc() == null || request.ruc().isBlank()) {
            throw new BusinessException("El RUC es obligatorio");
        }
        if (request.ruc() != null && !request.ruc().matches("^(10|20)\\d{9}$")) {
            throw new BusinessException("El RUC debe tener 11 dígitos y empezar con 10 o 20");
        }
        if (request.telefono() == null || !request.telefono().matches("\\d{9}")) {
            throw new BusinessException("El teléfono debe tener 9 dígitos");
        }
        if (usuarioRepository.existsByTelefono(request.telefono())) {
            throw new BusinessException("El número de teléfono ya está registrado");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("La contraseña es obligatoria");
        }
        validatePasswordStrength(request.password());

        Role role = roleRepository.findByNombre("ROLE_MYPE")
                .orElseThrow(() -> new BusinessException(
                        "Rol ROLE_MYPE no encontrado. Contacte al administrador."));

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .telefono(request.telefono())
                .rol(role)
                .build());

        mypeRepository.save(Mype.builder()
                .usuario(usuario)
                .nombreComercial(request.nombreComercial())
                .razonSocial(request.razonSocial())
                .ruc(request.ruc())
                .rubro(request.rubro())
                .direccion(request.direccion())
                .build());

        return buildAuthResponseSinRefresh(usuario, role.getNombre());
    }

    public AuthResponse login(LoginRequest request) {
        checkLoginAttempts(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (Exception e) {
            registerFailedAttempt(request.email());
            throw new BusinessException("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
        }

        var usuario = usuarioRepository.findByEmailWithRole(request.email()).orElseThrow();

        // Limpiar intentos fallidos con try-catch
        try {
            loginAttemptRepository.deleteByEmail(request.email());
            log.debug("Intentos de login eliminados para email: {}", request.email());
        } catch (Exception e) {
            log.error("Error al eliminar intentos de login para {}: {}", request.email(), e.getMessage());
            // No lanzamos excepción, continuamos
        }

        if (configuracionService.isModoMantenimiento()
                && !usuario.getRol().getNombre().equals("ROLE_ADMIN")) {
            throw new BusinessException("Sistema en mantenimiento.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        long refreshExp = request.rememberMe() ? refreshExpirationRemember : refreshExpiration;
        String refreshTokenValue = UUID.randomUUID().toString();

        try {
            refreshTokenRepository.deleteAllByUserId(usuario.getId());
            log.debug("Refresh tokens anteriores eliminados para usuario ID: {}", usuario.getId());
            webSocketNotificationService.sendToUser(usuario.getId(), "/topic/session",
                    Map.of("type", "SESSION_EXPIRED_REMOTE", "message", "Tu sesión ha sido cerrada porque iniciaste sesión en otro dispositivo."));
        } catch (Exception e) {
            log.error("Error al eliminar refresh tokens para usuario {}: {}", usuario.getId(), e.getMessage());
        }

        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(usuario.getId())
                .expiryDate(LocalDateTime.now().plus(refreshExp, ChronoUnit.MILLIS))
                .revoked(false)
                .build());

        String rolNormalizado = normalizeRole(usuario.getRol().getNombre());
        String accessToken = jwtService.generateToken(
                buildUserDetails(usuario), Map.of("rol", rolNormalizado), accessExpiration);

        return new AuthResponse(accessToken, refreshTokenValue, "Bearer",
                usuario.getId(), usuario.getNombre(), usuario.getEmail(), rolNormalizado);
    }

    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new BusinessException("Refresh token inválido", HttpStatus.UNAUTHORIZED));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException("Refresh token expirado", HttpStatus.UNAUTHORIZED);
        }

        Usuario usuario = usuarioRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", HttpStatus.UNAUTHORIZED));

        if (!usuario.getActivo()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException("Usuario desactivado", HttpStatus.UNAUTHORIZED);
        }

        String rolNormalizado = normalizeRole(usuario.getRol().getNombre());
        String newAccessToken = jwtService.generateToken(
                buildUserDetails(usuario), Map.of("rol", rolNormalizado), accessExpiration);

        return new AuthResponse(newAccessToken, refreshTokenValue, "Bearer",
                usuario.getId(), usuario.getNombre(), usuario.getEmail(), rolNormalizado);
    }

    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        Usuario usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), usuario.getPassword())) {
            throw new BusinessException("La contraseña actual es incorrecta", HttpStatus.BAD_REQUEST);
        }

        usuario.setPassword(passwordEncoder.encode(request.newPassword()));
        usuarioRepository.save(usuario);

        refreshTokenRepository.deleteAllByUserId(usuario.getId());
    }

    @Transactional
    public void sendVerificationOtp(String email) {
        if (configuracionService.isModoMantenimiento()) {
            throw new BusinessException(
                    "Sistema en mantenimiento. Solo administradores pueden ingresar.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!verificationEnabled) return;

        passwordResetRepository.invalidatePreviousCodes(email);

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
        passwordResetRepository.save(PasswordReset.builder()
                .email(email)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());
        emailService.sendOtpEmail(email, otp, "Verificación de correo - Linkuy");
    }

    public boolean verifyOtp(String email, String otp) {
        if (configuracionService.isModoMantenimiento()) {
            throw new BusinessException(
                    "Sistema en mantenimiento. Solo administradores pueden ingresar.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (!verificationEnabled) return true;

        return passwordResetRepository
                .findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(email, otp, LocalDateTime.now())
                .map(reset -> {
                    reset.setUsed(true);
                    passwordResetRepository.save(reset);
                    return true;
                })
                .orElse(false);
    }

    private void checkLoginAttempts(String email) {
        Optional<LoginAttempt> lastAttempt =
                loginAttemptRepository.findFirstByEmailOrderByIdDesc(email);
        if (lastAttempt.isPresent()
                && lastAttempt.get().getBlockedUntil() != null
                && lastAttempt.get().getBlockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(
                    "Demasiados intentos fallidos. Intente más tarde.", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private void registerFailedAttempt(String email) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(5);
        long attempts = loginAttemptRepository.countByEmailAndAttemptTimeAfter(email, windowStart);
        LocalDateTime blockedUntil = (attempts + 1 >= maxAttempts)
                ? LocalDateTime.now().plusMinutes(blockDurationMinutes)
                : null;
        loginAttemptRepository.save(LoginAttempt.builder()
                .email(email)
                .attemptTime(LocalDateTime.now())
                .blockedUntil(blockedUntil)
                .build());
    }

    private AuthResponse buildAuthResponseSinRefresh(Usuario usuario, String rolNombre) {
        String rolParaFrontend = normalizeRole(rolNombre);
        String token = jwtService.generateToken(
                buildUserDetails(usuario), Map.of("rol", rolParaFrontend), accessExpiration);
        return new AuthResponse(token, null, "Bearer",
                usuario.getId(), usuario.getNombre(), usuario.getEmail(), rolParaFrontend);
    }

    private String normalizeRole(String rolNombre) {
        return rolNombre.startsWith("ROLE_") ? rolNombre.substring(5) : rolNombre;
    }

    private User buildUserDetails(Usuario usuario) {
        return new User(usuario.getEmail(), usuario.getPassword(),
                List.of(new SimpleGrantedAuthority(usuario.getRol().getNombre())));
    }
}
