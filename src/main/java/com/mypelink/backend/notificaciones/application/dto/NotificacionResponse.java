package com.mypelink.backend.notificaciones.application.dto;

import com.mypelink.backend.shared.domain.enums.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        String urlReferencia,
        Boolean leida,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaLectura
) {}
