package com.rutadelsabor.core.models.enums;

public enum TipoDocumentoVenta {
    // R7-2: sin gate, disponible en plan básico (comprobante interno sin valor tributario)
    NOTA_VENTA,
    // R7-3: stub — gate FACTURACION, sin integración SUNAT real en esta fase
    BOLETA,
    FACTURA
}
