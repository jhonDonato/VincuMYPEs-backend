package com.mypelink.backend.certificaciones.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmitirCertificadoRequest(
        @NotNull(message = "El ID del proyecto es obligatorio")
        Long proyectoId,
        
        @NotNull(message = "El ID del estudiante es obligatorio")
        Long estudianteId,
        
        @NotBlank(message = "El título del certificado es obligatorio")
        @Size(max = 300, message = "El título no puede superar los 300 caracteres")
        String tituloCertificado,
        
        String descripcionCertificado,
        
        @Size(max = 255, message = "La URL del certificado no puede superar los 255 caracteres")
        String urlCertificado
) {}
