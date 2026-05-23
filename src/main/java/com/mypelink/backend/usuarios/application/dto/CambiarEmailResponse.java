package com.mypelink.backend.usuarios.application.dto;

public record CambiarEmailResponse(
        UsuarioResponse usuario,
        String nuevoToken
) {}