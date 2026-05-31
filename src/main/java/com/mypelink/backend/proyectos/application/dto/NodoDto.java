package com.mypelink.backend.proyectos.application.dto;

import java.util.List;

public record NodoDto(
        String pregunta,
        boolean tieneInputLibre,
        String inputPlaceholder,
        List<OpcionDto> opciones
) {}
