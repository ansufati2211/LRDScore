package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PagoItemDTO {
    private String metodoPago; 
    private BigDecimal monto;
    private String numeroYape;
    private String ultimosDigitos;
    private String titular;
}