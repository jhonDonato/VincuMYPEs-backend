package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.InsumoTipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsumoTipoRepository extends JpaRepository<InsumoTipo, Long> {
    List<InsumoTipo> findByTipoProyectoIdOrderByOrdenAsc(Long tipoProyectoId);
    List<InsumoTipo> findByTipoProyectoIdAndObligatorioTrue(Long tipoProyectoId);
}
