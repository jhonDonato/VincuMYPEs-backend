package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.NotNull;

public record IniciarVotacionRequest(
        @NotNull(message = "El ID del proyecto es obligatorio")
        Long proyectoId
) {}