package com.mypelink.backend.proyectos.application.dto;

public record TipoProyectoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcionMype,
        String descripcionEstudiante,
        String rama,
        Integer cicloMinimo,
        Boolean incluyePresupuesto,
        Boolean incluyeCronograma
) {}