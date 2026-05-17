package com.mypelink.backend.ejecucion.application.dto;

import com.mypelink.backend.shared.domain.enums.EstadoEntregable;
import java.time.LocalDateTime;

public record EntregableResponse(
        Long id,
        Long proyectoId,
        String proyectoTitulo,
        Long estudianteId,
        String estudianteNombre,
        String titulo,
        String descripcion,
        String archivo,
        EstadoEntregable estado,
        String observaciones,
        LocalDateTime fechaEntrega
) {}