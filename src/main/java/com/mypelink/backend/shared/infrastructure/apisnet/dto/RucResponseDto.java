package com.mypelink.backend.shared.infrastructure.apisnet.dto;

import lombok.Data;

@Data
public class RucResponseDto {

    private String ruc;

    private String razon_social;

    private String estado;

    private String condicion;

    private String direccion;

    private String departamento;

    private String provincia;

    private String distrito;

    private String mensaje;

    private String code;
}