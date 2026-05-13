package com.mypelink.backend.usuarios.application.dto;

import jakarta.validation.constraints.Size;

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
        String universidad
) {}
