package com.mypelink.backend.usuarios.application.dto;

public record CambiarEmailRequest(String emailNuevo, String passwordActual) {}