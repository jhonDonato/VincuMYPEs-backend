package com.mypelink.backend.comunicacion.domain.repository;

import com.mypelink.backend.comunicacion.domain.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ConversacionRepository extends JpaRepository<Conversacion, Long> {

    @Query("SELECT c FROM Conversacion c JOIN FETCH c.estudiante e JOIN FETCH e.usuario JOIN FETCH c.proyecto WHERE c.mypeUsuario.id = :usuarioId AND c.activo = true ORDER BY c.fechaUltimoMensaje DESC NULLS LAST")
    List<Conversacion> findByMypeUsuarioId(@Param("usuarioId") Long usuarioId);
    // ✅ NUEVO
    @Query("SELECT c FROM Conversacion c " +
            "JOIN FETCH c.proyecto " +
            "JOIN FETCH c.estudiante e " +
            "JOIN FETCH e.usuario " +
            "WHERE c.estudiante.id = :estudianteId " +
            "AND c.activo = true")
    List<Conversacion> findByEstudianteId(@Param("estudianteId") Long estudianteId);

    @Query("SELECT c FROM Conversacion c WHERE c.proyecto.id = :proyectoId AND c.estudiante.id = :estudianteId")
    Optional<Conversacion> findByProyectoIdAndEstudianteId(@Param("proyectoId") Long proyectoId, @Param("estudianteId") Long estudianteId);

}