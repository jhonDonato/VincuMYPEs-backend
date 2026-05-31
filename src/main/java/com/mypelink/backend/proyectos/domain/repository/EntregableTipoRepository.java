package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.EntregableTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EntregableTipoRepository extends JpaRepository<EntregableTipo, Long> {
    List<EntregableTipo> findByTipoProyectoIdOrderByOrdenAsc(Long tipoProyectoId);
    void deleteByTipoProyectoId(Long tipoProyectoId);
    Optional<EntregableTipo> findByTipoProyectoIdAndOrden(Long tipoProyectoId, Integer orden);
}