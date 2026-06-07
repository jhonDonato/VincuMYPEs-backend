package com.mypelink.backend.usuarios.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(String email, String password, boolean rememberMe) {}