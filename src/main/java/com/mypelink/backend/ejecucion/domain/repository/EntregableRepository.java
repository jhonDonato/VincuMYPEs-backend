package com.mypelink.backend.ejecucion.domain.repository;

import com.mypelink.backend.ejecucion.domain.model.Entregable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EntregableRepository extends JpaRepository<Entregable, Long> {

    @Query("SELECT e FROM Entregable e JOIN FETCH e.proyecto JOIN FETCH e.estudiante es JOIN FETCH es.usuario WHERE e.proyecto.id = :proyectoId ORDER BY e.fechaEntrega DESC")
    List<Entregable> findByProyectoIdWithDetails(@Param("proyectoId") Long proyectoId);

    @Query("SELECT e FROM Entregable e JOIN FETCH e.proyecto JOIN FETCH e.estudiante es JOIN FETCH es.usuario WHERE es.id = :estudianteId ORDER BY e.fechaEntrega DESC")
    List<Entregable> findByEstudianteIdWithDetails(@Param("estudianteId") Long estudianteId);
}