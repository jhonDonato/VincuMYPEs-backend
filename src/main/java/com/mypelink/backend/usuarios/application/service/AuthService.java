package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final MypeRepository mypeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse registerEstudiante(RegisterEstudianteRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("El email ya está registrado");
        }

        Role role = roleRepository.findByNombre("ESTUDIANTE")
                .orElseGet(() -> roleRepository.save(
                        Role.builder().nombre("ESTUDIANTE").descripcion("Estudiante universitario").build()));

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre(request.nombre())
                .email(request.email())
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
            throw new BusinessException("El email ya está registrado");
        }
        if (request.ruc() != null && mypeRepository.existsByRuc(request.ruc())) {
            throw new BusinessException("El RUC ya está registrado");
        }

        Role role = roleRepository.findByNombre("MYPE")
                .orElseGet(() -> roleRepository.save(
                        Role.builder().nombre("MYPE").descripcion("Microempresa").build()));

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
        var userDetails = new User(
                usuario.getEmail(),
                usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + rolNombre))
        );
        String token = jwtService.generateToken(userDetails, Map.of("rol", rolNombre));
        return new AuthResponse(token, "Bearer", usuario.getId(), usuario.getNombre(), usuario.getEmail(), rolNombre);
    }
}
