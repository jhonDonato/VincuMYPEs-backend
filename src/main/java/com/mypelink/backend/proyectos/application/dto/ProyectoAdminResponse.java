package com.mypelink.backend.proyectos.application.dto;

import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;

import java.time.LocalDateTime;

public record ProyectoAdminResponse(
        Long id,
        String titulo,
        AreaSistemas areaSistemas,
        WorkflowEstado estado,
        Integer cuposTotales,
        Long cuposAceptados,
        LocalDateTime fechaCreacion,
        String mypeNombre,
        Long mypeId,
        Boolean gestionCedida
) {
}