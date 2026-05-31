package com.mypelink.backend.proyectos.application.dto;

public record AdminOpcionResponse(
        Long id,
        String texto,
        Integer orden,
        Long nodoDestinoId,
        String nodoDestinoCodigo,
        Long tipoProyectoId,
        String tipoProyectoCodigo,
        boolean activo
) {}