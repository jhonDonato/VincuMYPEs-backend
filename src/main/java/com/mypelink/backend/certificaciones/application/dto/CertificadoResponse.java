package com.mypelink.backend.certificaciones.application.dto;

import java.time.LocalDate;

public record CertificadoResponse(
        Long id,
        Long proyectoId,
        String proyectoTitulo,
        Long estudianteId,
        String estudianteNombre,
        String codigo,
        String tituloCertificado,
        String descripcionCertificado,
        LocalDate fechaEmision,
        String urlCertificado
) {}
