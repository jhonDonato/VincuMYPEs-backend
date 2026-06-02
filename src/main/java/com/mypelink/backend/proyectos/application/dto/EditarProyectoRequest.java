package com.mypelink.backend.proyectos.application.dto;

import com.mypelink.backend.shared.domain.enums.AreaSistemas;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EditarProyectoRequest {

    @NotBlank
    @Size(max = 200)
    private String titulo;

    @NotBlank
    private String descripcion;

    private String objetivo;

    private String requisitos;

    private String entregablesSugeridos;

    @NotNull
    private AreaSistemas areaSistemas;

    @NotNull
    private Integer cupos;

    private LocalDate fechaInicio;

    private LocalDate fechaLimite;

    private Integer diasEstimados;
}