package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    boolean existsByProyectoIdAndEstudianteId(Long proyectoId, Long estudianteId);

    @Query("SELECT COUNT(p) FROM Postulacion p WHERE p.estudiante.id = :estudianteId AND p.estado = :estado")
    long countByEstudianteIdAndEstado(@Param("estudianteId") Long estudianteId, @Param("estado") EstadoPostulacion estado);

    List<Postulacion> findByProyectoId(Long proyectoId);

    Optional<Postulacion> findByProyectoIdAndEstudianteId(Long proyectoId, Long estudianteId);

    @Query("SELECT p FROM Postulacion p " +
            "JOIN FETCH p.proyecto " +
            "JOIN FETCH p.estudiante e " +
            "JOIN FETCH e.usuario " +
            "WHERE p.proyecto.id = :proyectoId")
    List<Postulacion> findByProyectoIdWithDetails(@Param("proyectoId") Long proyectoId);

    @Query("SELECT p FROM Postulacion p " +
            "JOIN FETCH p.proyecto " +
            "JOIN FETCH p.estudiante e " +
            "JOIN FETCH e.usuario " +
            "WHERE e.id = :estudianteId " +
            "ORDER BY p.fechaPostulacion DESC")
    List<Postulacion> findByEstudianteIdWithDetails(@Param("estudianteId") Long estudianteId);

    @Query("SELECT p FROM Postulacion p " +
            "JOIN FETCH p.proyecto " +
            "JOIN FETCH p.estudiante e " +
            "JOIN FETCH e.usuario " +
            "WHERE p.proyecto.id = :proyectoId AND p.estado = :estado")
    List<Postulacion> findByProyectoIdAndEstadoWithDetails(
            @Param("proyectoId") Long proyectoId,
            @Param("estado") EstadoPostulacion estado
    );

    @Query("SELECT p FROM Postulacion p " +
            "JOIN FETCH p.proyecto proj " +
            "JOIN FETCH proj.mype m " +
            "JOIN FETCH m.usuario " +
            "JOIN FETCH p.estudiante e " +
            "JOIN FETCH e.usuario " +
            "WHERE p.estado = :estado " +
            "AND p.fechaLimiteConfirmacion IS NOT NULL " +
            "AND p.fechaLimiteConfirmacion < :ahora")
    List<Postulacion> findExpiradasEnEstado(
            @Param("estado") EstadoPostulacion estado,
            @Param("ahora") LocalDateTime ahora
    );

    @Query("SELECT p FROM Postulacion p " +
            "JOIN FETCH p.proyecto " +
            "WHERE p.estudiante.id = :estudianteId " +
            "AND p.estado = :estado")
    List<Postulacion> findByEstudianteIdAndEstado(
            @Param("estudianteId") Long estudianteId,
            @Param("estado") EstadoPostulacion estado
    );
}
