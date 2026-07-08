package com.rutadelsabor.core.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RecetaRequestDTO {
    private Long insumoId;
    private BigDecimal cantidad;
    private String unidadMedida;
}