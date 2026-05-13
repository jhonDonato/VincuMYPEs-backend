package com.mypelink.backend.usuarios.application.dto;

public record EstudianteProfileResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String fotoPerfil,
        String codigoEstudiante,
        String universidad,
        String carrera,
        String bio,
        String skills,
        String portafolioUrl,
        String linkedinUrl
) {}
