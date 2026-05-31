package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record EntregableTipoRequest(
        @NotBlank String titulo,
        String descripcion,
        Integer orden
) {}