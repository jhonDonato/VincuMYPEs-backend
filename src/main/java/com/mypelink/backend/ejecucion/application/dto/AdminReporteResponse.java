// com.mypelink.backend.ejecucion.application.dto.AdminReporteResponse
package com.mypelink.backend.ejecucion.application.dto;

import java.util.List;

public record AdminReporteResponse(
        List<ReporteItemResponse> reportes,
        double promedioGeneral,
        double promedioMypeAEstudiante,
        double promedioEstudianteAMype,
        long totalCalificaciones,
        List<AreaDistribucionResponse> distribucionAreas
) {}