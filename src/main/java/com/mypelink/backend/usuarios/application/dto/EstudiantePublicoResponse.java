package com.mypelink.backend.usuarios.application.dto;

import java.math.BigDecimal;

/**
 * Response del perfil público de un estudiante.
 * Los campos sensibles (email, telefono, codigoEstudiante, barrio, lat, lng)
 * se devuelven SOLO cuando el viewer es ADMIN. Para MYPE y otros estudiantes
 * vienen como null.
 */
public record EstudiantePublicoResponse(
        Long id,
        String nombre,
        String fotoPerfil,
        String universidad,
        String carrera,
        String bio,
        String skills,
        String portafolioUrl,
        String linkedinUrl,
        String cvUrl,
        String ciudad,
        String sector,
        String pais,

        // ─── Solo visibles para ADMIN ───
        String email,
        String telefono,
        String codigoEstudiante,
        String barrio,
        BigDecimal lat,
        BigDecimal lng,

        Long usuarioId   // ← NUEVO
) {}