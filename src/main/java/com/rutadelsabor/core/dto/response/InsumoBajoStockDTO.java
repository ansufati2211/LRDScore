package com.rutadelsabor.core.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InsumoBajoStockDTO {
    private Long insumoId;
    private String nombre;
    private String unidadMedida;
    private Long sedeId;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
}