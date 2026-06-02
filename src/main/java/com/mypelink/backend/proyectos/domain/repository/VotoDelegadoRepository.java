package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.VotoDelegado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface VotoDelegadoRepository extends JpaRepository<VotoDelegado, Long> {

    // Verificar si un estudiante ya votó en esta votación
    boolean existsByVotacionIdAndVotanteId(Long votacionId, Long votanteId);

    // Contar votos por candidato
    @Query("SELECT COUNT(v) FROM VotoDelegado v " +
            "WHERE v.votacion.id = :votacionId " +
            "AND v.candidato.id = :candidatoId")
    long countVotosByCandidato(@Param("votacionId") Long votacionId,
                               @Param("candidatoId") Long candidatoId);

    // Todos los votos de una votación
    @Query("SELECT v FROM VotoDelegado v " +
            "JOIN FETCH v.votante vt " +
            "JOIN FETCH vt.usuario " +
            "JOIN FETCH v.candidato c " +
            "JOIN FETCH c.usuario " +
            "WHERE v.votacion.id = :votacionId")
    List<VotoDelegado> findByVotacionIdWithDetails(@Param("votacionId") Long votacionId);

    // Buscar el voto de un estudiante específico
    Optional<VotoDelegado> findByVotacionIdAndVotanteId(Long votacionId, Long votanteId);
}