package com.mypelink.backend.ejecucion.application.dto;

import java.util.List;

public record AdminReporteResponse(
        List<EvaluacionDetalleResponse> reportes,
        Double satisfaccionPromedio,
        Integer tiempoPromedio,
        Double tasaExito,
        long totalMypes,
        long estudiantesActivos,
        long proyectosEnDesarrollo,
        long totalEvaluaciones,
        List<AreaDistribucionResponse> distribucionAreas
) {}
