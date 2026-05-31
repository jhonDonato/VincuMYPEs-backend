package com.mypelink.backend.proyectos.domain.repository;
import com.mypelink.backend.proyectos.domain.model.InsumoProyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsumoProyectoRepository extends JpaRepository<InsumoProyecto, Long> {
    List<InsumoProyecto> findByProyectoId(Long proyectoId);
    long countByProyectoIdAndInsumoTipoId(Long proyectoId, Long insumoTipoId);
}
