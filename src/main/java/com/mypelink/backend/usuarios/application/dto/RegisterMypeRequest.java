package com.mypelink.backend.usuarios.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterMypeRequest(
        @NotBlank @Size(min = 3, max = 100) String nombre,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        String telefono,
        @NotBlank String nombreComercial,
        String razonSocial,
        String ruc,
        String rubro,
        String direccion
) {}