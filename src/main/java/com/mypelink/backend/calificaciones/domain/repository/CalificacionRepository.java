// com.mypelink.backend.calificaciones.domain.repository.CalificacionRepository
package com.mypelink.backend.calificaciones.domain.repository;

import com.mypelink.backend.calificaciones.domain.model.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}