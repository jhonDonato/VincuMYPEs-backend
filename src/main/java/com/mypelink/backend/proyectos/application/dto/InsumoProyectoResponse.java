package com.mypelink.backend.proyectos.application.dto;

public record InsumoProyectoResponse(
        Long id,
        Long proyectoId,
        Long insumoTipoId,
        String nombreInsumo,
        String valorTexto,
        String archivoUrl
) {}
