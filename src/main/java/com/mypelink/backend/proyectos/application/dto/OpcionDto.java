package com.mypelink.backend.proyectos.application.dto;

public record OpcionDto(
        String texto,
        String siguiente,   // codigo del nodo destino, o null si es hoja
        String resultado    // codigo del tipo proyecto, o null si lleva a otra pregunta
) {}
