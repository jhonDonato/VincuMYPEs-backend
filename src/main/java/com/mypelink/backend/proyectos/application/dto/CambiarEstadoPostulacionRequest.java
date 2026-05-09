package com.mypelink.backend.proyectos.application.dto;

import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoPostulacionRequest(
        @NotNull EstadoPostulacion estado
) {}