package com.mypelink.backend.shared.application.service;

import com.mypelink.backend.shared.application.dto.ModoMantenimientoRequest;
import com.mypelink.backend.shared.application.service.ConfiguracionService;
import com.mypelink.backend.shared.domain.model.ConfiguracionSistema;
import com.mypelink.backend.shared.domain.repository.ConfiguracionSistemaRepository;
import com.mypelink.backend.usuarios.domain.model.Role;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceTest {

    @Mock private ConfiguracionSistemaRepository configRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ConfiguracionService configuracionService;

    private ConfiguracionSistema config;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        config = ConfiguracionSistema.builder()
                .id(1L).modoMantenimiento(false).build();
        admin = Usuario.builder().id(1L).nombre("Admin")
                .email("admin@test.com")
                .rol(Role.builder().nombre("ROLE_ADMIN").build())
                .build();
    }

    @Test
    void isModoMantenimiento_ReturnsFalse() {
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));
        assertFalse(configuracionService.isModoMantenimiento());
    }

    @Test
    void isModoMantenimiento_ReturnsTrue() {
        config.setModoMantenimiento(true);
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));
        assertTrue(configuracionService.isModoMantenimiento());
    }

    @Test
    void obtenerModoMantenimiento_Success() {
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));
        var response = configuracionService.obtenerModoMantenimiento();
        assertFalse(response.modoMantenimiento());
    }

    @Test
    void actualizarModoMantenimiento_Success() {
        when(configRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(config));
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(configRepository.save(any(ConfiguracionSistema.class))).thenReturn(config);

        var response = configuracionService.actualizarModoMantenimiento(
                new ModoMantenimientoRequest(true), "admin@test.com");

        assertTrue(response.modoMantenimiento());
        assertTrue(config.getModoMantenimiento());
    }
}
