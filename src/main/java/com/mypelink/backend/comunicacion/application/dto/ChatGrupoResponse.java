package com.mypelink.backend.comunicacion.application.dto;

import com.mypelink.backend.shared.domain.enums.TipoConversacion;
import java.time.LocalDateTime;

public record ChatGrupoResponse(
        Long id,
        Long proyectoId,
        String proyectoTitulo,
        TipoConversacion tipo,
        String nombre,
        String ultimoMensaje,
        LocalDateTime fechaUltimoMensaje,
        int totalMiembros,
        int mensajesNoLeidos
) {}