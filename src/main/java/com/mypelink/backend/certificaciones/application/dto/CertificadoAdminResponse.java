package com.mypelink.backend.certificaciones.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CertificadoAdminResponse(
        Long id,
        String codigo,
        String tituloCertificado,
        String descripcionCertificado,
        String nombreEstudiante,
        String emailEstudiante,
        String nombreProyecto,
        String nombreMype,
        String urlCertificado,
        LocalDate fechaEmision,
        LocalDateTime fechaEnvio
) {}