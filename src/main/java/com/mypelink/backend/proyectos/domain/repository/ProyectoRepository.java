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

    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype m WHERE p.mype.id = :mypeId ORDER BY p.fechaCreacion DESC")
    List<Proyecto> findByMypeIdConMype(@Param("mypeId") Long mypeId);

    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype m WHERE p.estado = :estado AND p.activo = true")
    List<Proyecto> findPublicosConMype(@Param("estado") WorkflowEstado estado);

    @Query(value = "SELECT COUNT(p) FROM Proyecto p WHERE p.estado = :estado AND p.activo = true")
    long countByEstadoAndActivoTrue(@Param("estado") WorkflowEstado estado);

    @Query("SELECT p FROM Proyecto p WHERE p.mype.id = :mypeId")
    List<Proyecto> findByMypeId(@Param("mypeId") Long mypeId);

    @Query("SELECT p FROM Proyecto p JOIN FETCH p.mype m JOIN FETCH m.usuario WHERE p.id = :id")
    java.util.Optional<Proyecto> findByIdWithMype(@Param("id") Long id);
}