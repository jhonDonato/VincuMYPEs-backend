package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoProyectoRepository extends JpaRepository<TipoProyecto, Long> {
    List<TipoProyecto> findByActivoTrue();
    List<TipoProyecto> findByRamaAndActivoTrue(String rama);
    Optional<TipoProyecto> findByCodigo(String codigo);
}