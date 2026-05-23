package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.jwt.JwtService;
import com.mypelink.backend.usuarios.application.dto.*;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;  // ← NUEVO

    @Transactional
    public UsuarioResponse actualizarInfo(String email, ActualizarInfoRequest request) {
        var usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        if (request.nombre() != null && !request.nombre().isBlank()) {
            usuario.setNombre(request.nombre());
        }
        if (request.telefono() != null) {
            usuario.setTelefono(request.telefono());
        }
        usuarioRepository.save(usuario);

        return new UsuarioResponse(usuario.getId(), usuario.getNombre(),
                usuario.getEmail(), usuario.getTelefono(),
                usuario.getFotoPerfil(), usuario.getRol().getNombre());
    }

    @Transactional
    public void cambiarPassword(String email, CambiarPasswordRequest request) {
        var usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new BusinessException("La contraseña actual es incorrecta", HttpStatus.BAD_REQUEST);
        }
        if (request.passwordNueva().length() < 8) {
            throw new BusinessException("La nueva contraseña debe tener al menos 8 caracteres", HttpStatus.BAD_REQUEST);
        }

        usuario.setPassword(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public CambiarEmailResponse cambiarEmail(String emailActual, CambiarEmailRequest request) {
        var usuario = usuarioRepository.findByEmailWithRole(emailActual)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new BusinessException("La contraseña es incorrecta", HttpStatus.BAD_REQUEST);
        }
        if (usuarioRepository.existsByEmail(request.emailNuevo())) {
            throw new BusinessException("Este email ya está en uso", HttpStatus.BAD_REQUEST);
        }

        usuario.setEmail(request.emailNuevo());
        usuarioRepository.save(usuario);

        // Generar nuevo token con el email actualizado
        // Construimos un UserDetails temporal con el nuevo email
        var userDetails = User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(usuario.getRol().getNombre())))
                .build();

        String nuevoToken = jwtService.generateToken(userDetails, Map.of());

        return new CambiarEmailResponse(
                new UsuarioResponse(usuario.getId(), usuario.getNombre(),
                        usuario.getEmail(), usuario.getTelefono(),
                        usuario.getFotoPerfil(), usuario.getRol().getNombre()),
                nuevoToken
        );
    }

    @Transactional
    public void desactivarCuenta(String email, ConfirmarPasswordRequest request) {
        var usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new BusinessException("La contraseña es incorrecta", HttpStatus.BAD_REQUEST);
        }

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }
}