package com.mypelink.backend.proyectos.application.dto;

public record ValidacionItem(
        String tipo,
        String mensaje,
        Long nodoId,
        Long opcionId
) {}