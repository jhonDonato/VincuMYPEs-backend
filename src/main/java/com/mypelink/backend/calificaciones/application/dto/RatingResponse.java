// com.mypelink.backend.calificaciones.application.dto.RatingResponse
package com.mypelink.backend.calificaciones.application.dto;

public record RatingResponse(
        Double promedio,
        Long cantidad
) {}