package com.mypelink.backend.proyectos.application.dto;

public record InsumoTipoResponse(
        Long id,
        Long tipoProyectoId,
        String nombre,
        String descripcion,
        String formato,
        Boolean obligatorio,
        Integer orden
) {}
