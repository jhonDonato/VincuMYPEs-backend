package com.mypelink.backend.certificaciones.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
public record EmitirCertificadoRequest(
        @NotNull Long proyectoId,
        @NotNull @Size(min = 1) List<@NotNull Long> estudiantesIds,
        @NotBlank String tituloCertificado,
        String descripcionCertificado,
        String firmaBase64,
        String gerenteNombre,
        // ✅ Asegurar que estos campos existen
        String proyectoTitulo,
        String estudianteNombre,
        String mypeNombre
) {}