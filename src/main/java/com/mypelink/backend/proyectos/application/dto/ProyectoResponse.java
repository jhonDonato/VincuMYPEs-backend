package com.mypelink.backend.proyectos.application.dto;

import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProyectoResponse(
        Long id,
        String titulo,
        String descripcion,
        String objetivo,
        String requisitos,
        String entregablesSugeridos,
        AreaSistemas areaSistemas,
        WorkflowEstado estado,
        Integer cupos,
        LocalDate fechaInicio,
        LocalDate fechaLimite,
        LocalDateTime fechaCreacion,
        String mypeNombre,
        Long mypeId
) {}