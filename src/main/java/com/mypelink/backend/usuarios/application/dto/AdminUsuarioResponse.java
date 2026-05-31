package com.mypelink.backend.usuarios.application.dto;

public record AdminUsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        String estado,
        String carrera,
        String sector,
        Integer limiteProyectos,
        Double promedioEstrellas,
        Long proyectosCompletados
) {}
