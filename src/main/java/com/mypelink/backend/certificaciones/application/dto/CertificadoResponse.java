package com.mypelink.backend.certificaciones.application.dto;

import java.time.LocalDate;

public record CertificadoResponse(
        Long id,
        String codigo,
        String tituloCertificado,
        String descripcionCertificado,
        String estudianteNombre,
        String emailEstudiante,
        String proyectoTitulo,
        String mypeNombre,
        String urlCertificado,
        LocalDate fechaEmision,
        Boolean enviadoEmail
) {}
