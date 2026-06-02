package com.mypelink.backend.usuarios.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarLimiteProyectosRequest(
        @NotNull(message = "El nuevo límite es obligatorio")
        @Min(value = 1, message = "El límite mínimo es 1")
        @Max(value = 5, message = "El límite máximo permitido es 5")
        Integer nuevoLimite
) {}