package com.mypelink.backend.comunicacion.application.dto;
import java.time.LocalDateTime;

public record MensajeResponse(
        Long id,
        Long remitenteId,
        String remitenteNombre,
        String mensaje,
        String archivoAdjunto,
        LocalDateTime fechaEnvio,
        boolean leido,
        boolean esMio  // true si el remitente es quien consulta
) {}