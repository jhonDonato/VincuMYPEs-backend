package com.mypelink.backend.comunicacion.application.dto;

import com.mypelink.backend.shared.domain.enums.TipoConversacion;
import jakarta.validation.constraints.NotNull;

public record CrearChatGrupoRequest(
        @NotNull(message = "El ID del proyecto es obligatorio")
        Long proyectoId,

        @NotNull(message = "El tipo de chat es obligatorio")
        TipoConversacion tipo,

        String nombre
) {}