package com.mypelink.backend.proyectos.application.dto;

import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import java.time.LocalDateTime;

public record PostulacionResponse(
        Long id,
        Long proyectoId,
        String proyectoTitulo,
        Long estudianteId,
        String estudianteNombre,
        EstadoPostulacion estado,
        String mensajePostulacion,
        LocalDateTime fechaPostulacion,
        String estudianteCvUrl,
        LocalDateTime fechaLimiteConfirmacion,
        boolean estudianteOcupado,
        Boolean esDelegado,
        // ✅ NUEVOS
        WorkflowEstado proyectoEstado,
        Integer cupos,
        LocalDateTime proyectoFechaInicioReal,
        String proyectoArea,
        // ✅ NUEVOS
        LocalDateTime proyectoFechaFin,
        java.util.List<String> integrantes

) {}