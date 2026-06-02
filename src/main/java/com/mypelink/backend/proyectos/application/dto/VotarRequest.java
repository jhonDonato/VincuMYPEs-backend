package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.NotNull;

public record VotarRequest(
        @NotNull(message = "El ID del candidato es obligatorio")
        Long candidatoId
) {}