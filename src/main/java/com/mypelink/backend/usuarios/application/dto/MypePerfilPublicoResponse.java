package com.mypelink.backend.usuarios.application.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MypePerfilPublicoResponse {
    private Long id;
    private String nombreComercial;
    private String rubro;
    private String descripcion;
    private String sitioWeb;
    private String instagram;
    private String facebook;
    private String tiktok;
    private String whatsapp;
    private String direccion;
    private String ciudad;
    private String sector;
    private Double latitud;
    private Double longitud;
}