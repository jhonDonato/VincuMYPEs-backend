package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    Page<Proyecto> findByEstadoAndActivoTrue(WorkflowEstado estado, Pageable pageable);

    @Query("SELECT p FROM Proyecto p WHERE p.mype.id = :mypeId AND p.activo = true")
    List<Proyecto> findByMypeId(@Param("mypeId") Long mypeId);

    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype m JOIN FETCH m.usuario WHERE p.id = :id AND p.activo = true")
    java.util.Optional<Proyecto> findByIdWithMype(@Param("id") Long id);
}
