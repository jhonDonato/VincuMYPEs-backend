package com.mypelink.backend.usuarios.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterEstudianteRequest(
        @NotBlank @Size(min = 3, max = 100) String nombre,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        String telefono,
        String codigoEstudiante,
        String carrera,
        String universidad
) {}
