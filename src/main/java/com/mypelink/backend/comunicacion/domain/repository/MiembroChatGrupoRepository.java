package com.mypelink.backend.comunicacion.domain.repository;

import com.mypelink.backend.comunicacion.domain.model.MiembroChatGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MiembroChatGrupoRepository extends JpaRepository<MiembroChatGrupo, Long> {

    // Verificar si un usuario ya es miembro del chat
    boolean existsByChatGrupoIdAndUsuarioId(Long chatGrupoId, Long usuarioId);

    // Todos los miembros de un chat
    @Query("SELECT m FROM MiembroChatGrupo m " +
            "JOIN FETCH m.usuario " +
            "WHERE m.chatGrupo.id = :chatGrupoId")
    List<MiembroChatGrupo> findByChatGrupoIdWithUsuario(@Param("chatGrupoId") Long chatGrupoId);

    // Quitar un miembro
    void deleteByChatGrupoIdAndUsuarioId(Long chatGrupoId, Long usuarioId);
}