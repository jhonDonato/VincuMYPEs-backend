package com.mypelink.backend.usuarios.application.dto;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String fotoPerfil,
        String rol
) {}
