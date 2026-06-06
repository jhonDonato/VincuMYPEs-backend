package com.mypelink.backend.shared.application.dto;

import java.util.Map;

public record DashboardStatsResponse(
        long totalEstudiantes,
        long totalMypes,
        long totalAdmins,
        long proyectosActivos,
        long proyectosCompletados,
        long postulacionesPendientes,
        long certificadosEmitidos,
        Double promedioCalificacionMypes,
        Double promedioCalificacionEstudiantes,
        Map<String, Long> proyectosPorArea
) {}
