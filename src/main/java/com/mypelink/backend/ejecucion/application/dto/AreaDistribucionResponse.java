package com.mypelink.backend.ejecucion.application.dto;

public record AreaDistribucionResponse(
        String area,
        String label,
        long cantidad,
        double porcentaje
) {}
