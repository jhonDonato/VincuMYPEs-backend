package com.mypelink.backend.usuarios.application.dto;

public record CambiarPasswordRequest(String passwordActual, String passwordNueva) {}