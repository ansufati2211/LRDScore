package com.rutadelsabor.core.models.enums;

public enum Modulo {

    PEDIDOS(true),
    CAJA(true),
    INVENTARIO(true),
    KDS(true),
    REPORTES_AVANZADOS(false),
    FACTURACION(false),
    RESERVAS(false),
    FIDELIZACION(false);

    private final boolean esCore;

    Modulo(boolean esCore) {
        this.esCore = esCore;
    }

    public boolean esCore() {
        return esCore;
    }
}
