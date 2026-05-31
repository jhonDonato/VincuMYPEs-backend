package com.mypelink.backend.proyectos.application.dto;

import java.util.List;

public record ResultadoDto(
        String codigo,
        String titulo,
        String areaSistemas,
        Integer cuposMin,
        Integer cuposMax,
        Integer diasMin,
        Integer diasSugerido,
        String descripcionMype,
        List<EntregableTipoResponse> entregables,
        Long tipoProyectoId
) {}
