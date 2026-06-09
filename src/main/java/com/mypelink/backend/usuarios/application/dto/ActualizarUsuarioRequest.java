package com.mypelink.backend.usuarios.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
        @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
        String nombre,

        @Email(message = "Formato de email inválido")
        String email,

        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener 9 dígitos")
        String telefono,

        @Pattern(regexp = "ESTUDIANTE|MYPE|ADMIN", message = "Rol inválido")
        String rol,

        // Solo para estudiantes
        String codigoEstudiante,
        String carrera,
        String universidad,
        Integer limiteProyectos,

        // Solo para MYPE
        String nombreComercial,
        String razonSocial,
        String ruc,
        String rubro,
        String direccion,
        String descripcion,
        String sitioWeb,
        String instagram,
        String facebook,
        String tiktok,
        String whatsapp
) {}