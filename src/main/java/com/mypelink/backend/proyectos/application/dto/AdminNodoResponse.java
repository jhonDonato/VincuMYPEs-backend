package com.mypelink.backend.proyectos.application.dto;

import java.util.List;

public record AdminNodoResponse(
        Long id,
        String codigo,
        String pregunta,
        boolean tieneInputLibre,
        String inputPlaceholder,
        boolean esRaiz,
        Integer orden,
        boolean activo,
        List<AdminOpcionResponse> opciones
) {}