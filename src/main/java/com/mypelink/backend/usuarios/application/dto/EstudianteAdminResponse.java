package com.mypelink.backend.usuarios.application.dto;

public record EstudianteAdminResponse(
        Long id,
        String nombre,
        String email,
        String codigoEstudiante,
        String universidad,
        String carrera,
        Integer limiteProyectos,
        long proyectosActivos,
        Boolean activo
) {}