package com.mypelink.backend.usuarios.application.dto;

import com.mypelink.backend.proyectos.application.dto.ProyectoResponse;
import java.util.List;

public record MypePerfilResponse(
        Long id,

        // ── Siempre públicos ─────────────────────────────────
        String nombreComercial,
        String nombreRepresentante,
        String razonSocial,
        String rubro,
        String fotoPerfil,
        String descripcion,

        // Redes sociales y web — siempre públicos
        String sitioWeb,
        String instagram,
        String facebook,
        String tiktok,
        String whatsapp,

        // ── Solo si tiene acceso (PROPIO o CONFIRMADO) ───────
        String ruc,
        String direccion,
        String telefono,
        String emailContacto,

        // ── Meta ─────────────────────────────────────────────
        String nivelAcceso,   // "PROPIO" | "CONFIRMADO" | "PUBLICO"
        Long totalProyectos,
        Long proyectosActivos,

        // Lista de proyectos — filtrada según nivel de acceso
        List<ProyectoResponse> proyectos
) {}