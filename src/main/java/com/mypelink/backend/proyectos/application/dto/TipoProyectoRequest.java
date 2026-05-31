package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TipoProyectoRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        String descripcionMype,
        String descripcionEstudiante,
        @NotBlank String rama,
        @NotNull Integer cicloMinimo,
        String areaSistemas,
        Integer cuposMin,
        Integer cuposMax,
        Integer diasMin,
        Integer diasSugerido,
        String complejidad,
        Integer esfuerzoHPers,
        String alcanceIncluye,
        String alcanceNoIncluye
) {}