// com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository
package com.mypelink.backend.calificaciones.domain.repository;

import com.mypelink.backend.calificaciones.domain.model.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    boolean existsByProyectoIdAndCalificadorIdAndCalificadoId(
            Long proyectoId, Long calificadorId, Long calificadoId
    );

    @Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.calificado.id = :usuarioId")
    Double promedioDeUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(c) FROM Calificacion c WHERE c.calificado.id = :usuarioId")
    long cantidadDeUsuario(@Param("usuarioId") Long usuarioId);

    List<Calificacion> findByProyectoIdAndCalificadorId(Long proyectoId, Long calificadorId);

    @Query("SELECT AVG(c.puntuacion) FROM Calificacion c WHERE c.calificado.rol.nombre = :rolNombre")
    Double promedioByCalificadoRol(@Param("rolNombre") String rolNombre);

    @Query("""
    SELECT c.id, c.createdAt, p.titulo, calif.id, califdo.nombre, c.puntuacion,
           p.fechaInicio, p.fechaLimite
    FROM Calificacion c
    JOIN c.proyecto p
    JOIN c.calificador calif
    JOIN c.calificado califdo
    WHERE calif.rol.nombre = 'ROLE_MYPE' AND califdo.rol.nombre = 'ROLE_ESTUDIANTE'
    ORDER BY c.createdAt DESC
""")
    List<Object[]> findReportesMypeAEstudiante();

    // Calificaciones donde MYPE califica a ESTUDIANTE (ya lo tienes casi igual)
    @Query("""
    SELECT c.id, c.createdAt, p.titulo, calif.id, califdo.nombre, c.puntuacion
    FROM Calificacion c
    JOIN c.proyecto p
    JOIN c.calificador calif
    JOIN c.calificado califdo
    WHERE calif.rol.nombre = 'ROLE_MYPE' AND califdo.rol.nombre = 'ROLE_ESTUDIANTE'
    ORDER BY c.createdAt DESC
""")
    List<Object[]> findMypeAEstudiante();

    @Query("""
    SELECT c.id, c.createdAt, p.titulo, calif.nombre, califdo.id, c.puntuacion
    FROM Calificacion c
    JOIN c.proyecto p
    JOIN c.calificador calif
    JOIN c.calificado califdo
    WHERE calif.rol.nombre = 'ROLE_ESTUDIANTE' AND califdo.rol.nombre = 'ROLE_MYPE'
    ORDER BY c.createdAt DESC
""")

    List<Object[]> findEstudianteAMype();

    // Promedios segmentados
    @Query("SELECT COALESCE(AVG(c.puntuacion), 0.0) FROM Calificacion c WHERE c.calificador.rol.nombre = 'ROLE_MYPE' AND c.calificado.rol.nombre = 'ROLE_ESTUDIANTE'")
    Double promedioMypeAEstudiante();

    @Query("SELECT COALESCE(AVG(c.puntuacion), 0.0) FROM Calificacion c WHERE c.calificador.rol.nombre = 'ROLE_ESTUDIANTE' AND c.calificado.rol.nombre = 'ROLE_MYPE'")
    Double promedioEstudianteAMype();

    @Query("SELECT COALESCE(AVG(c.puntuacion), 0.0) FROM Calificacion c")
    Double promedioGeneral();

    // Agregar al CalificacionRepository.java
    @Query("SELECT c FROM Calificacion c " +
            "JOIN FETCH c.proyecto p " +
            "JOIN FETCH p.mype m " +
            "JOIN FETCH m.usuario " +
            "JOIN FETCH c.calificador calif " +
            "JOIN FETCH calif.rol " +
            "JOIN FETCH c.calificado califdo " +
            "JOIN FETCH califdo.rol " +
            "WHERE c.proyecto.id = :proyectoId " +
            "AND c.calificador.id = :calificadorId " +
            "AND c.calificado.id = :calificadoId")
    Optional<Calificacion> findByProyectoIdAndCalificadorIdAndCalificadoId(
            @Param("proyectoId") Long proyectoId,
            @Param("calificadorId") Long calificadorId,
            @Param("calificadoId") Long calificadoId);
}

