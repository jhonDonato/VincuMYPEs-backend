package com.mypelink.backend.ejecucion.application.dto;

import java.util.List;

public record AdminReporteResponse(
        List<EvaluacionDetalleResponse> reportes,
        Double satisfaccionPromedio,
        Integer tiempoPromedio, // en días
        Double tasaExito // porcentaje, ej. 92.0
) {}
