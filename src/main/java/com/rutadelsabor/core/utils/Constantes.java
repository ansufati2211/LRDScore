package com.rutadelsabor.core.utils;

public final class Constantes {
    
    private Constantes() {
        // Previene que alguien instancie esta clase
    }

    // ROLES
    public static final String ROL_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROL_CAJERO = "ROLE_CAJERO";
    public static final String ROL_MOZO = "ROLE_MOZO";
    public static final String ROL_COCINERO = "ROLE_COCINERO";

    // ESTADOS DE PEDIDO
// ESTADOS DE PEDIDO
    public static final String ESTADO_PEDIDO_RECIBIDO = "RECIBIDO";
    public static final String ESTADO_PEDIDO_EN_PREPARACION = "EN_PREPARACION"; 
    public static final String ESTADO_PEDIDO_LISTO = "LISTO";
    public static final String ESTADO_PEDIDO_ENTREGADO = "ENTREGADO";
    public static final String ESTADO_PEDIDO_CANCELADO = "CANCELADO";

    // MÉTODOS DE PAGO
    public static final String METODO_PAGO_YAPE = "YAPE";
    public static final String METODO_PAGO_PLIN = "PLIN";
    public static final String METODO_PAGO_EFECTIVO = "EFECTIVO";
    public static final String METODO_PAGO_TARJETA = "TARJETA";
}