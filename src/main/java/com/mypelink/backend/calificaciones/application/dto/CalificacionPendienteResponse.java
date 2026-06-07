// com.mypelink.backend.calificaciones.application.dto.CalificacionPendienteResponse
package com.mypelink.backend.calificaciones.application.dto;

public record CalificacionPendienteResponse(
        Long proyectoId,
        String proyectoTitulo,
        Long calificadoId,
        String calificadoNombre,
        String calificadoFotoPerfil,
        String tipoCalificado  // "MYPE" o "ESTUDIANTE"
) {}