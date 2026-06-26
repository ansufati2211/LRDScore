package com.rutadelsabor.core.models.enums;

// R0-1: módulos core = PEDIDOS, CAJA, INVENTARIO, KDS
// R0-1: módulos premium = REPORTES_AVANZADOS, FACTURACION, RESERVAS, FIDELIZACION
// E0-1: suscripción VENCIDA → core pasa a solo lectura, premium bloqueado
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
