// com.mypelink.backend.calificaciones.application.dto.CalificacionAdminResponse.java
package com.mypelink.backend.calificaciones.application.dto;

import java.time.LocalDateTime;

public record CalificacionAdminResponse(
        Long id,
        Long proyectoId,
        String proyectoTitulo,
        Long calificadorId,
        String calificadorNombre,
        String calificadorRol,
        Long calificadoId,
        String calificadoNombre,
        String calificadoRol,
        Integer puntuacion,
        LocalDateTime createdAt,
        String enlaceProyecto
) {}