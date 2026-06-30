package com.mypelink.backend.certificaciones.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EnviarCertificadoConCalificacionRequest(
        String pdfBase64,
        @NotNull @Min(1) @Max(5) Integer calificacion
) {}
