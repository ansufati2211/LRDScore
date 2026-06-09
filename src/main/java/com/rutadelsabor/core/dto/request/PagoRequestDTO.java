package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PagoRequestDTO {
    private Long sesionCajaId;
    private String metodoPago; // Ya es String, mapea perfecto a la BD
    private BigDecimal monto;  // Estandarizado como 'monto'
    private String numeroYape;
    private String ultimosDigitos;
    private String titular;
}