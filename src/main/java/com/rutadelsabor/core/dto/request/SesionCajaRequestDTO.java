package com.rutadelsabor.core.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SesionCajaRequestDTO {
    private BigDecimal montoInicial;
    private BigDecimal montoFinalDeclarado;
    private Long sedeId; // <-- FIX: Propiedad añadida para que el controlador lo detecte
}