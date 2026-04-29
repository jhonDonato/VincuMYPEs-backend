package com.mypelink.backend.proyectos.application.dto;

import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;

import java.time.LocalDateTime;

public record PostulacionResponse(
        Long id,
        Long proyectoId,
        String proyectoTitulo,
        Long estudianteId,
        String estudianteNombre,
        EstadoPostulacion estado,
        String mensajePostulacion,
        LocalDateTime fechaPostulacion
) {}
