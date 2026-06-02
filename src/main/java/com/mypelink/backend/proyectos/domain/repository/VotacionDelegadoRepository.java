package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.VotacionDelegado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VotacionDelegadoRepository extends JpaRepository<VotacionDelegado, Long> {

    // Buscar votación activa de un proyecto
    @Query("SELECT v FROM VotacionDelegado v " +
            "JOIN FETCH v.proyecto " +
            "WHERE v.proyecto.id = :proyectoId " +
            "AND v.estado = 'EN_VOTACION'")
    Optional<VotacionDelegado> findActivaByProyectoId(@Param("proyectoId") Long proyectoId);

    // Buscar si ya existe una votación para este proyecto
    boolean existsByProyectoId(Long proyectoId);

    // Buscar votación con ganador
    @Query("SELECT v FROM VotacionDelegado v " +
            "JOIN FETCH v.postulacionGanadora pg " +
            "JOIN FETCH pg.estudiante e " +
            "JOIN FETCH e.usuario " +
            "WHERE v.proyecto.id = :proyectoId " +
            "AND v.estado = 'COMPLETADA'")
    Optional<VotacionDelegado> findCompletadaByProyectoId(@Param("proyectoId") Long proyectoId);
}