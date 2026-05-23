package com.mypelink.backend.usuarios.application.dto;

import java.math.BigDecimal;

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
        String linkedinUrl,
        String cvUrl,

        // 📍 NUEVOS CAMPOS DE UBICACIÓN
        String ciudad,
        String pais,
        String sector,
        String barrio,
        BigDecimal lat,
        BigDecimal lng
) {}