package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.auth.recovery.application.service.EmailService;
import com.mypelink.backend.auth.recovery.domain.model.PasswordReset;
import com.mypelink.backend.auth.recovery.domain.repository.PasswordResetRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.ThreadLocalRandom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${app.email.verification.enabled:false}")
    private boolean verificationEnabled;

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final MypeRepository mypeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetRepository passwordResetRepository;
    private final EmailService emailService;

    @Transactional
    public AuthResponse registerEstudiante(RegisterEstudianteRequest request) {
        if (verificationEnabled) {
            if (!request.email().toLowerCase().endsWith("@upn.pe")) {
                throw new BusinessException("Solo se permiten correos institucionales @upn.pe");
            }
            if (request.codigoEstudiante() == null || request.codigoEstudiante().isBlank()) {
                throw new BusinessException("El código de estudiante es obligatorio");
            }
        }

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo electrónico ya está registrado en otra cuenta.");
        }
        if (request.dni() != null && usuarioRepository.existsByDni(request.dni())) {
            throw new BusinessException("Este DNI ya se encuentra registrado.");
        }
        if (request.codigoEstudiante() != null && estudianteRepository.existsByCodigoEstudiante(request.codigoEstudiante())) {
            throw new BusinessException("Este código de estudiante ya ha sido utilizado.");
        }

        Role role = roleRepository.findByNombre("ROLE_ESTUDIANTE")
                .orElseThrow(() -> new BusinessException("Rol ROLE_ESTUDIANTE no encontrado. Contacte al administrador."));

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
                .carrera(request.carrera() != null ? request.carrera() : "Ingeniería de Sistemas Computacionales")
                .universidad(request.universidad() != null ? request.universidad() : "Universidad Privada del Norte")
                .build());

        return buildAuthResponse(usuario, role.getNombre());
    }

    @Transactional
    public AuthResponse registerMype(RegisterMypeRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("El correo electrónico ya está registrado en otra cuenta.");
        }
        if (request.ruc() != null && mypeRepository.existsByRuc(request.ruc())) {
            throw new BusinessException("Este RUC ya se encuentra registrado.");
        }

        Role role = roleRepository.findByNombre("ROLE_MYPE")
                .orElseThrow(() -> new BusinessException("Rol ROLE_MYPE no encontrado. Contacte al administrador."));

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

        return buildAuthResponse(usuario, role.getNombre());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        var usuario = usuarioRepository.findByEmailWithRole(request.email()).orElseThrow();
        return buildAuthResponse(usuario, usuario.getRol().getNombre());
    }

    private AuthResponse buildAuthResponse(Usuario usuario, String rolNombre) {
        String rolParaFrontend = rolNombre.startsWith("ROLE_") ? rolNombre.substring(5) : rolNombre;
        var userDetails = new User(
                usuario.getEmail(),
                usuario.getPassword(),
                List.of(new SimpleGrantedAuthority(rolNombre))
        );
        String token = jwtService.generateToken(userDetails, Map.of("rol", rolParaFrontend));
        return new AuthResponse(token, "Bearer", usuario.getId(), usuario.getNombre(), usuario.getEmail(), rolParaFrontend);
    }

    @Transactional
    public void sendVerificationOtp(String email) {
        if (!verificationEnabled) return;

        passwordResetRepository.invalidatePreviousCodes(email);

        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));

        PasswordReset reset = PasswordReset.builder()
                .email(email)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        passwordResetRepository.save(reset);

        emailService.sendOtpEmail(email, otp, "Verificación de correo - Linkuy");
    }

    public boolean verifyOtp(String email, String otp) {
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
}
