package com.mypelink.backend.proyectos.application.dto;

public record EntregableTipoResponse(
        Long id,
        Long tipoProyectoId,
        String titulo,
        String descripcion,
        Integer orden
) {}