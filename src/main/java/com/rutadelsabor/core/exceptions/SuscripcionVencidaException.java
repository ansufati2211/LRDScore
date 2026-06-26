package com.rutadelsabor.core.exceptions;

import com.rutadelsabor.core.models.enums.Modulo;

// E0-1: suscripción VENCIDA + módulo core + operación de escritura → 403 SUSCRIPCION_VENCIDA
public class SuscripcionVencidaException extends RuntimeException {

    private final Modulo modulo;

    public SuscripcionVencidaException(Modulo modulo) {
        super("La suscripción ha vencido. El módulo " + modulo.name() + " está disponible solo en modo lectura");
        this.modulo = modulo;
    }

    public Modulo getModulo() {
        return modulo;
    }
}
