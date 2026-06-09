// com.mypelink.backend.usuarios.application.dto.UsuarioDetailAdminResponse.java
package com.mypelink.backend.usuarios.application.dto;

import java.time.LocalDateTime;

public record UsuarioDetailAdminResponse(
        Long id,
        String nombre,
        String email,
        String dni,
        String telefono,
        String fotoPerfil,
        String rol,
        Boolean activo,
        Boolean emailVerified,
        LocalDateTime fechaRegistro,
        LocalDateTime ultimaSesion,

        // Datos específicos de Estudiante
        EstudianteInfo estudiante,

        // Datos específicos de MYPE
        MypeInfo mype
) {
    public record EstudianteInfo(
            Long id,
            String codigoEstudiante,
            String universidad,
            String carrera,
            String bio,
            String skills,
            String portafolioUrl,
            String linkedinUrl,
            String cvUrl,
            Integer limiteProyectos,
            String ciudad,
            String pais,
            String sector
    ) {}

    public record MypeInfo(
            Long id,
            String nombreComercial,
            String razonSocial,
            String nombreRepresentante,
            String ruc,
            String rubro,
            String direccion,
            String telefono,
            String emailContacto,
            String descripcion,
            String sitioWeb,
            String instagram,
            String facebook,
            String tiktok,
            String whatsapp,
            String ciudad,
            String sector
    ) {}
}