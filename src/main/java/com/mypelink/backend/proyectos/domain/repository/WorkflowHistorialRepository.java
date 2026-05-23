package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.WorkflowHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowHistorialRepository extends JpaRepository<WorkflowHistorial, Long> {

    @Query("SELECT w FROM WorkflowHistorial w JOIN FETCH w.proyecto p JOIN FETCH w.cambiadoPor u JOIN FETCH u.rol r ORDER BY w.fechaCambio DESC")
    List<WorkflowHistorial> findAllWithDetails();

    @Query("SELECT w FROM WorkflowHistorial w JOIN FETCH w.cambiadoPor u JOIN FETCH u.rol r WHERE w.proyecto.id = :proyectoId ORDER BY w.fechaCambio DESC")
    List<WorkflowHistorial> findByProyectoId(@Param("proyectoId") Long proyectoId);
}