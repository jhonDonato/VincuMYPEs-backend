// com.mypelink.backend.ejecucion.application.dto.ReporteItemResponse
package com.mypelink.backend.ejecucion.application.dto;

import java.time.LocalDateTime;

public record ReporteItemResponse(
        String id,                   // "CAL-109-143"
        LocalDateTime fecha,         // fecha de la calificación
        String proyecto,             // título del proyecto
        String calificador,          // nombre del que califica
        String calificado,           // nombre del calificado
        String tipo,                 // "MYPE a Estudiante" o "Estudiante a MYPE"
        int puntuacion               // 1-5
) {}