package com.mypelink.backend.proyectos.application.dto;

import java.time.LocalDateTime;

public record PostulacionAdminResponse(
        Long id,
        Long estudianteId,
        String estudianteNombre,
        String estudianteEmail,
        Long proyectoId,
        String proyectoTitulo,
        String proyectoArea,
        Long mypeId,
        String mypeNombre,
        LocalDateTime fechaPostulacion,
        String estado,
        LocalDateTime fechaRespuesta,
        LocalDateTime fechaLimiteConfirmacion,
        boolean tienePreseleccionado
) {}
