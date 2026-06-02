package com.mypelink.backend.comunicacion.domain.repository;

import com.mypelink.backend.comunicacion.domain.model.MensajeGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MensajeGrupoRepository extends JpaRepository<MensajeGrupo, Long> {

    // Todos los mensajes de un chat ordenados por fecha
    @Query("SELECT m FROM MensajeGrupo m " +
            "JOIN FETCH m.remitente " +
            "WHERE m.chatGrupo.id = :chatGrupoId " +
            "ORDER BY m.fechaEnvio ASC")
    List<MensajeGrupo> findByChatGrupoId(@Param("chatGrupoId") Long chatGrupoId);

    // Último mensaje del chat (para vista previa)
    @Query("SELECT m FROM MensajeGrupo m " +
            "WHERE m.chatGrupo.id = :chatGrupoId " +
            "ORDER BY m.fechaEnvio DESC")
    List<MensajeGrupo> findUltimoMensaje(@Param("chatGrupoId") Long chatGrupoId);
}