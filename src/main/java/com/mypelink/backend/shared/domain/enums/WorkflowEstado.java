package com.mypelink.backend.shared.domain.enums;

public enum WorkflowEstado {
    BORRADOR,
    PENDIENTE,
    EN_VOTACION_DELEGADO,   // ← nuevo
    EN_DESARROLLO,
    EN_REVISION,
    COMPLETADO,
    CANCELADO,
    PENDIENTE_ADMIN,        // ← nuevo
    VACANTES_ABIERTAS       // ← nuevo
}