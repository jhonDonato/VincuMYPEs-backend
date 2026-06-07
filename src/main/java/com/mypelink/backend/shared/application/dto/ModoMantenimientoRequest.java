package com.mypelink.backend.shared.application.dto;

import jakarta.validation.constraints.NotNull;

public record ModoMantenimientoRequest(@NotNull Boolean modoMantenimiento) {}