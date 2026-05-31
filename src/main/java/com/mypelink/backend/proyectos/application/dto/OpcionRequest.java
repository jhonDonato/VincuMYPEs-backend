package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpcionRequest(
        @NotBlank String texto,
        @NotNull Integer orden,
        Long nodoDestinoId,
        Long tipoProyectoId,
        @NotNull Boolean activo
) {}