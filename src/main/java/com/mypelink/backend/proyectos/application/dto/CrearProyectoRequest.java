package com.mypelink.backend.proyectos.application.dto;

import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CrearProyectoRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank String descripcion,
        String objetivo,
        String requisitos,
        String entregablesSugeridos,
        @NotNull AreaSistemas areaSistemas,
        Integer cupos,
        LocalDate fechaInicio,
        LocalDate fechaLimite
) {}
