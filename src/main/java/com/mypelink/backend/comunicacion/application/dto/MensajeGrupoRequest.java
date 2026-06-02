package com.mypelink.backend.comunicacion.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MensajeGrupoRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 2000, message = "Máximo 2000 caracteres")
        String mensaje,

        String archivoAdjunto
) {}