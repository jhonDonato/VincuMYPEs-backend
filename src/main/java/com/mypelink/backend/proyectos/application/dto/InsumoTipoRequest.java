package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record InsumoTipoRequest(
        @NotBlank String nombre,
        String descripcion,
        String formato,
        Boolean obligatorio,
        Integer orden
) {}
