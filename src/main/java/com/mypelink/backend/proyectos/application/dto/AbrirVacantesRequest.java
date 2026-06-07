package com.mypelink.backend.proyectos.application.dto;

import java.util.List;

public record AbrirVacantesRequest(List<Long> estudianteIds) {}