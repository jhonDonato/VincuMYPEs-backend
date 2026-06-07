package com.mypelink.backend.shared.domain.repository;

import com.mypelink.backend.shared.domain.model.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionSistemaRepository extends JpaRepository<ConfiguracionSistema, Long> {

    // Como solo esperamos un registro, podemos obtenerlo por ID fijo (1) o con un método especial
    Optional<ConfiguracionSistema> findFirstByOrderByIdAsc();
}