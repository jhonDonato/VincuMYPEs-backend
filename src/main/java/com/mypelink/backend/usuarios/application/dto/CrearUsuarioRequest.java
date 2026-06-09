package com.mypelink.backend.usuarios.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefono,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "ESTUDIANTE|MYPE|ADMIN", message = "Rol inválido")
        String rol,

        // Solo para estudiantes
        String codigoEstudiante,
        String carrera,
        String universidad,

        // Solo para MYPE
        String nombreComercial,
        String razonSocial,
        String ruc,
        String rubro,
        String direccion
) {}