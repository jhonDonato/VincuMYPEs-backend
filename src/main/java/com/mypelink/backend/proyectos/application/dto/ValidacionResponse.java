package com.mypelink.backend.proyectos.application.dto;

import java.util.List;

public record ValidacionResponse(
        List<ValidacionItem> errores,
        List<ValidacionItem> warnings
) {}