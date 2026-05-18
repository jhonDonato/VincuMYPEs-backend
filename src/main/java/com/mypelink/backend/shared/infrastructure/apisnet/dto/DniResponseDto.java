package com.mypelink.backend.shared.infrastructure.apisnet.dto;

import lombok.Data;

@Data
public class DniResponseDto {

    private String dni;

    private String cliente;

    private String nombres;

    private String apellido_paterno;

    private String apellido_materno;

    private String mensaje;

    private String code;
}