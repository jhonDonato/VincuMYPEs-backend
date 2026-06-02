package com.mypelink.backend.proyectos.application.dto;

public record ResultadoVotacionResponse(
        Long votacionId,
        Long proyectoId,
        String proyectoTitulo,
        Long ganadorId,
        String ganadorNombre,
        String ganadorEmail,
        long totalVotos,
        boolean esEmpate,
        String mensaje
) {}