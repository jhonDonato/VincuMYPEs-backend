package com.mypelink.backend.comunicacion.application.dto;
import java.time.LocalDateTime;

public record ConversacionResponse(
        Long id,
        Long proyectoId,
        String proyectoTitulo,
        Long estudianteId,
        String estudianteNombre,
        String ultimoMensaje,
        LocalDateTime fechaUltimoMensaje,
        long mensajesNoLeidos
) {}