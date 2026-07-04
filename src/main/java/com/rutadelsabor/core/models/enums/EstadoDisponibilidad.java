package com.rutadelsabor.core.models.enums;

public enum EstadoDisponibilidad {
    DISPONIBLE,
    // R6-1: marcado desde KDS, reversible desde cocina en cualquier momento (E6-2)
    AGOTADO_TEMPORAL,
    // R6-2: marcado desde KDS, bloqueado hasta cierre de caja (R6-3/E6-3)
    AGOTADO_SERVICIO
}
