package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.Size;

public record PostulacionRequest(
        @Size(max = 200) String mensajePostulacion,
        String archivoAdjunto
) {}