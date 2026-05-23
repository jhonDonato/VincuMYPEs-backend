package com.mypelink.backend.usuarios.application.dto;

public record ActualizarMypeRequest(
        String rubro,
        String descripcion,
        String sitioWeb,
        String instagram,
        String facebook,
        String tiktok,
        String whatsapp,
        String direccion,
        String telefono,
        String emailContacto
) {}