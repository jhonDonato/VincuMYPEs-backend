package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Value("${app.email.verification.enabled:false}")
    private boolean verificationEnabled;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Cuenta desactivada → mensaje genérico
        if (!usuario.getActivo()) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        // Email no verificado (solo si la verificación está activada)
        if (verificationEnabled && !usuario.getEmailVerified()) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        return new User(
                usuario.getEmail(),
                usuario.getPassword(),
                List.of(new SimpleGrantedAuthority(usuario.getRol().getNombre()))
        );
    }
}