package com.mypelink.backend.shared.application.service;

import com.mypelink.backend.shared.application.dto.ModoMantenimientoRequest;
import com.mypelink.backend.shared.application.dto.ModoMantenimientoResponse;
import com.mypelink.backend.shared.domain.model.ConfiguracionSistema;
import com.mypelink.backend.shared.domain.repository.ConfiguracionSistemaRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionSistemaRepository configRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public boolean isModoMantenimiento() {
        return getConfiguracionUnica().getModoMantenimiento();
    }

    @Transactional(readOnly = true)
    public ModoMantenimientoResponse obtenerModoMantenimiento() {
        ConfiguracionSistema config = getConfiguracionUnica();
        return new ModoMantenimientoResponse(config.getModoMantenimiento());
    }

    @Transactional
    public ModoMantenimientoResponse actualizarModoMantenimiento(ModoMantenimientoRequest request, String emailAdmin) {
        Usuario admin = usuarioRepository.findByEmail(emailAdmin)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        ConfiguracionSistema config = getConfiguracionUnica();
        config.setModoMantenimiento(request.modoMantenimiento());
        config.setUpdatedBy(admin.getNombre());
        configRepository.save(config);

        return new ModoMantenimientoResponse(config.getModoMantenimiento());
    }

    private ConfiguracionSistema getConfiguracionUnica() {
        return configRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException(
                        "ConfiguracionSistema no inicializada. " +
                                "Verifique que ConfiguracionSistemaInitializer haya corrido al arrancar."
                ));
    }
}