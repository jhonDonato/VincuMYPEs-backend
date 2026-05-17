package com.mypelink.backend.ejecucion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntregableRequest(
        @NotBlank @Size(max = 200) String titulo,
        String descripcion,
        String archivo
) {}