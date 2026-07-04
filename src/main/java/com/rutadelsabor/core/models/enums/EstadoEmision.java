package com.rutadelsabor.core.models.enums;

public enum EstadoEmision {
    EMITIDO,
    // E7-3: anulación como estado; el comprobante nunca se elimina
    ANULADO,
    // Estados preparados para integración SUNAT/OSE futura (BOLETA / FACTURA)
    ENVIADO_SUNAT,
    ACEPTADO,
    RECHAZADO
}
