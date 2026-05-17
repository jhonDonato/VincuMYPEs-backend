package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.TipoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoProyectoRepository extends JpaRepository<TipoProyecto, Long> {
    List<TipoProyecto> findByActivoTrue();
    List<TipoProyecto> findByRamaAndActivoTrue(String rama);
}