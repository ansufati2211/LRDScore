package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class InsumoRequestDTO {
    private String nombre;
    private String unidadMedida; // Ej: kg, gr, ml, und
    private BigDecimal stockActual;
}