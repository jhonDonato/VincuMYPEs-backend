package com.mypelink.backend.comunicacion.application.dto;

public record CrearConversacionRequest(
        Long proyectoId,
        String mensaje
) {}