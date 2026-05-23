package com.mypelink.backend.ejecucion.application.dto;

import java.time.LocalDateTime;

public record EvaluacionDetalleResponse(
        String id,
        String proyecto,
        String mype,
        String estudiante,
        LocalDateTime fechaCierre,
        Integer duracionDias,
        Double calificacionMype,
        String comentarioMype
) {}
