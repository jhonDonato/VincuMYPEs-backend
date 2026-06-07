package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostulacionRepository
        extends JpaRepository<Postulacion, Long>,
                JpaSpecificationExecutor<Postulacion> {

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
    @Query("SELECT COUNT(p) FROM Postulacion p " +
            "JOIN p.proyecto pr " +
            "WHERE p.estudiante.id = :estudianteId " +
            "AND p.estado = :estado " +
            "AND pr.estado IN :proyectoEstados")
    long countByEstudianteIdAndEstadoAndProyectoEstadoIn(
            @Param("estudianteId") Long estudianteId,
            @Param("estado") EstadoPostulacion estado,
            @Param("proyectoEstados") List<WorkflowEstado> proyectoEstados);

    @Query("SELECT p FROM Postulacion p " +
            "JOIN FETCH p.proyecto pr " +
            "JOIN FETCH pr.mype m " +
            "JOIN FETCH m.usuario " +
            "JOIN FETCH p.estudiante e " +
            "JOIN FETCH e.usuario " +
            "WHERE p.estudiante.id = :estudianteId " +
            "AND p.estado IN :estados " +
            "AND p.proyecto.id <> :proyectoIdExcluir")
    List<Postulacion> findByEstudianteIdAndEstadoInExcluyendoProyecto(
            @Param("estudianteId") Long estudianteId,
            @Param("estados") List<EstadoPostulacion> estados,
            @Param("proyectoIdExcluir") Long proyectoIdExcluir);

    @Query("SELECT COUNT(p) > 0 FROM Postulacion p " +
            "WHERE p.estudiante.id = :estudianteId " +
            "AND p.proyecto.mype.id = :mypeId")
    boolean existsPostulacionDeEstudianteEnProyectoDeMype(
            @Param("estudianteId") Long estudianteId,
            @Param("mypeId") Long mypeId);

    @Query("SELECT COUNT(p1) > 0 FROM Postulacion p1, Postulacion p2 " +
            "WHERE p1.proyecto.id = p2.proyecto.id " +
            "AND p1.estudiante.id = :estudianteAId " +
            "AND p2.estudiante.id = :estudianteBId " +
            "AND p1.estado = :estadoConfirmado " +
            "AND p2.estado = :estadoConfirmado " +
            "AND p1.proyecto.estado = :estadoEnDesarrollo")
    boolean compartenProyectoEnDesarrollo(
            @Param("estudianteAId") Long estudianteAId,
            @Param("estudianteBId") Long estudianteBId,
            @Param("estadoConfirmado") EstadoPostulacion estadoConfirmado,
            @Param("estadoEnDesarrollo") WorkflowEstado estadoEnDesarrollo);

    @Query("SELECT COUNT(p) FROM Postulacion p " +
            "WHERE p.proyecto.id = :proyectoId AND p.estado = :estado")


    long countByProyectoIdAndEstado(
            @Param("proyectoId") Long proyectoId,
            @Param("estado") EstadoPostulacion estado);

    boolean existsByProyectoIdAndEstudianteUsuarioIdAndEstado(Long proyectoId, Long estudianteUsuarioId, EstadoPostulacion estado);
    @Query("SELECT COUNT(p) > 0 FROM Postulacion p " +
            "WHERE p.proyecto.id = :proyectoId " +
            "AND p.estudiante.id = :estudianteId " +
            "AND p.estado IN :estadosActivos")
    boolean existsPostulacionActiva(@Param("proyectoId") Long proyectoId,
                                    @Param("estudianteId") Long estudianteId,
                                    @Param("estadosActivos") List<EstadoPostulacion> estadosActivos);
    List<Postulacion> findByProyectoIdAndEstado(Long proyectoId, EstadoPostulacion estado);

    List<Postulacion> findByEstudianteUsuarioIdAndEstado(Long estudianteUsuarioId, EstadoPostulacion estado);

    @Query("SELECT COUNT(p) FROM Postulacion p WHERE p.estado = :estado")
    long countByEstado(@Param("estado") EstadoPostulacion estado);

    @Query("SELECT DISTINCT p.proyecto.id FROM Postulacion p " +
           "WHERE p.proyecto.id IN :proyectoIds AND p.estado = :estado")
    List<Long> findProyectoIdsConEstado(
            @Param("proyectoIds") List<Long> proyectoIds,
            @Param("estado") EstadoPostulacion estado);
}
