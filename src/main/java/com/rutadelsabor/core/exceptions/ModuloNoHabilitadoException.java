package com.rutadelsabor.core.exceptions;

import com.rutadelsabor.core.models.enums.Modulo;

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
