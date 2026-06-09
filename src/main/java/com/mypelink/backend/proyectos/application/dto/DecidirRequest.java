package com.mypelink.backend.proyectos.application.dto;

import jakarta.validation.constraints.NotNull;

public record DecidirRequest(
        @NotNull Decision decision,
        Integer diasExtra
) {}