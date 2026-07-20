package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class RecetaRequestDTO {
    private Long productoId;
    private Long insumoId;
    private BigDecimal cantidadRequerida;
    private String unidadMedida;
}