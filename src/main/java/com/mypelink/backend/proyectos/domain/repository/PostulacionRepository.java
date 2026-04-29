package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    boolean existsByProyectoIdAndEstudianteId(Long proyectoId, Long estudianteId);

    @Query("SELECT COUNT(p) FROM Postulacion p WHERE p.estudiante.id = :estudianteId AND p.estado = :estado")
    long countByEstudianteIdAndEstado(@Param("estudianteId") Long estudianteId, @Param("estado") EstadoPostulacion estado);

    List<Postulacion> findByProyectoId(Long proyectoId);

    Optional<Postulacion> findByProyectoIdAndEstudianteId(Long proyectoId, Long estudianteId);
}
