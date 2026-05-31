package com.mypelink.backend.proyectos.application.dto;

import java.util.List;

public record AdminArbolResponse(
        List<AdminNodoResponse> nodos
) {}