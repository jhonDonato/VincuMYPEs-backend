package com.mypelink.backend.proyectos.application.dto;

import java.util.List;
import java.util.Map;

public record ArbolDecisionResponse(
        String nodoRaizCodigo,
        Map<String, NodoDto> nodos,
        Map<String, ResultadoDto> resultados

) {}