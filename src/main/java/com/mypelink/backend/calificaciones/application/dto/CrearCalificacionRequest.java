// com.mypelink.backend.calificaciones.application.dto.CrearCalificacionRequest
package com.mypelink.backend.calificaciones.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearCalificacionRequest(
        @NotNull Long proyectoId,
        @NotNull Long calificadoId,
        @NotNull @Min(1) @Max(5) Integer puntuacion
) {}