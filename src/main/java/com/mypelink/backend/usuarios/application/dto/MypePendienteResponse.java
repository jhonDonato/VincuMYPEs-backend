package com.mypelink.backend.usuarios.application.dto;

import java.time.LocalDateTime;

public record MypePendienteResponse(
        Long id,
        Long usuarioId,
        String nombreUsuario,
        String email,
        String nombreComercial,
        String razonSocial,
        String ruc,
        String rubro,
        String estado,
        LocalDateTime fechaRegistro
) {}
