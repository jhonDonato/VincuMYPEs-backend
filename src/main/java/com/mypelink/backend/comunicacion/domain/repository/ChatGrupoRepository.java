package com.mypelink.backend.comunicacion.domain.repository;

import com.mypelink.backend.comunicacion.domain.model.ChatGrupo;
import com.mypelink.backend.shared.domain.enums.TipoConversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatGrupoRepository extends JpaRepository<ChatGrupo, Long> {

    // Chats de equipo de un proyecto
    @Query("SELECT c FROM ChatGrupo c " +
            "WHERE c.proyecto.id = :proyectoId " +
            "AND c.tipo = :tipo")
    Optional<ChatGrupo> findByProyectoIdAndTipo(@Param("proyectoId") Long proyectoId,
                                                @Param("tipo") TipoConversacion tipo);

    // Todos los chats de un proyecto
    List<ChatGrupo> findByProyectoId(Long proyectoId);

    // Chats donde un usuario es miembro
    @Query("SELECT c FROM ChatGrupo c " +
            "JOIN MiembroChatGrupo m ON m.chatGrupo.id = c.id " +
            "WHERE m.usuario.id = :usuarioId")
    List<ChatGrupo> findByMiembroUsuarioId(@Param("usuarioId") Long usuarioId);
}