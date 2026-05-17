package com.mypelink.backend.ejecucion.application.dto;

import com.mypelink.backend.shared.domain.enums.EstadoEntregable;
import jakarta.validation.constraints.NotNull;

public record RevisarEntregableRequest(
        @NotNull EstadoEntregable estado,
        String observaciones
) {}