package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NodoRequest(
        @NotBlank String codigo,
        @NotBlank String pregunta,
        @NotNull Boolean tieneInputLibre,
        String inputPlaceholder,
        @NotNull Boolean esRaiz,
        @NotNull Integer orden,
        @NotNull Boolean activo
) {}