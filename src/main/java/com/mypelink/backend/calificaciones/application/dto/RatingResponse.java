// com.mypelink.backend.calificaciones.application.dto.RatingResponse
package com.mypelink.backend.calificaciones.application.dto;
import java.util.Map;


public record RatingResponse(
        Double promedio,
        Long cantidad,
        Map<Integer, Long> distribucion
) {}