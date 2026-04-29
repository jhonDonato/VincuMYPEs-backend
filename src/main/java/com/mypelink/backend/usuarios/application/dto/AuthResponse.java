package com.mypelink.backend.usuarios.application.dto;

public record AuthResponse(
        String token,
        String tipo,
        Long usuarioId,
        String nombre,
        String email,
        String rol
) {}
