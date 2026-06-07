package com.mypelink.backend.shared.infrastructure.config;

import com.mypelink.backend.shared.domain.model.ConfiguracionSistema;
import com.mypelink.backend.shared.domain.repository.ConfiguracionSistemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garantiza que la tabla configuracion_sistema tiene una fila al arrancar la app.
 * Si no existe, la crea con valores por defecto (mantenimiento OFF).
 * Esto evita que cada lectura intente hacer INSERT desde una transacción readOnly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionSistemaInitializer implements ApplicationRunner {

    private final ConfiguracionSistemaRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() == 0) {
            repository.save(ConfiguracionSistema.builder()
                    .modoMantenimiento(false)
                    .updatedBy("SISTEMA")
                    .build());
            log.info("ConfiguracionSistema inicializada con valores por defecto (mantenimiento OFF).");
        }
    }
}