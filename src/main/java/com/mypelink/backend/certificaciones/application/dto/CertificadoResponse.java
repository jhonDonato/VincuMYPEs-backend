package com.mypelink.backend.certificaciones.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CertificadoResponse(
        Long id,
        String codigo,
        String tituloCertificado,
        String descripcionCertificado,
        String estudianteNombre,
        String emailEstudiante,
        Long estudianteId,
        String nombreProyecto,
        Long proyectoId,
        String nombreMype,
        Long mypeUsuarioId,
        String urlCertificado,
        LocalDate fechaEmision,
        LocalDateTime fechaEnvio,
        // ✅ NUEVOS CAMPOS
        String gerenteNombre,
        String firmaUrl,
        String nombreRepresentante
) {}
