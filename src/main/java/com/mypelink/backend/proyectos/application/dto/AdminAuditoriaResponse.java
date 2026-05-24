package com.mypelink.backend.proyectos.application.dto;

import java.time.LocalDateTime;

public record AdminAuditoriaResponse(
        String id,
        String proyecto,
        String actor,
        String rolActor,
        String estadoAnterior,
        String estadoNuevo,
        LocalDateTime fecha,
        String comentario
) {}
