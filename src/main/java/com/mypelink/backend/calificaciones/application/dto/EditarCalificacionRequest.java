// com.mypelink.backend.calificaciones.application.dto.EditarCalificacionRequest.java
package com.mypelink.backend.calificaciones.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EditarCalificacionRequest(
        @NotNull @Min(1) @Max(5) Integer nuevaPuntuacion,
        String motivoEdicion
) {}