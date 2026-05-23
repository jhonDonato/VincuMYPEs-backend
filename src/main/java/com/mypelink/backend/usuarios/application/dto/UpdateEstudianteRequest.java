package com.mypelink.backend.usuarios.application.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateEstudianteRequest(
        String bio,
        String skills,

        @Size(max = 255, message = "La URL del portafolio no puede superar los 255 caracteres")
        String portafolioUrl,

        @Size(max = 255, message = "La URL de LinkedIn no puede superar los 255 caracteres")
        String linkedinUrl,

        @Size(max = 100, message = "La carrera no puede superar los 100 caracteres")
        String carrera,

        @Size(max = 100, message = "La universidad no puede superar los 100 caracteres")
        String universidad,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String telefono,

        // 📍 NUEVOS CAMPOS DE UBICACIÓN
        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        String ciudad,

        @Size(max = 100, message = "El país no puede superar los 100 caracteres")
        String pais,

        @Size(max = 100, message = "El sector no puede superar los 100 caracteres")
        String sector,

        @Size(max = 100, message = "El barrio no puede superar los 100 caracteres")
        String barrio,

        BigDecimal lat,

        BigDecimal lng
) {}