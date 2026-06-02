package com.mypelink.backend.proyectos.application.dto;

import com.mypelink.backend.shared.domain.enums.FaseVotacion;
import java.time.LocalDateTime;
import java.util.List;

public record VotacionResponse(
        Long id,
        Long proyectoId,
        String proyectoTitulo,
        FaseVotacion estado,
        LocalDateTime fechaLimite,
        LocalDateTime fechaCreacion,
        Long ganadorId,
        String ganadorNombre,
        List<CandidatoDto> candidatos,
        boolean yaVote,
        int totalVotos
) {
    public record CandidatoDto(
            Long estudianteId,
            String estudianteNombre,
            long votosRecibidos,
            boolean esGanador
    ) {}
}