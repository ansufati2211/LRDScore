package com.rutadelsabor.core.exceptions;

import com.rutadelsabor.core.models.enums.Modulo;

// E0-2: el plan del tenant no incluye este módulo → 403 MODULO_NO_HABILITADO
public class ModuloNoHabilitadoException extends RuntimeException {

    private final Modulo modulo;

    public ModuloNoHabilitadoException(Modulo modulo) {
        super("El módulo " + modulo.name() + " no está habilitado en el plan actual");
        this.modulo = modulo;
    }

    public Modulo getModulo() {
        return modulo;
    }
}
