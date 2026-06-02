package com.mypelink.backend.comunicacion.application.dto;

import java.time.LocalDateTime;

public record MensajeGrupoResponse(
        Long id,
        Long chatGrupoId,
        Long remitenteId,
        String remitenteNombre,
        String remitenteRol,
        String mensaje,
        String archivoAdjunto,
        LocalDateTime fechaEnvio,
        boolean esMio
) {}