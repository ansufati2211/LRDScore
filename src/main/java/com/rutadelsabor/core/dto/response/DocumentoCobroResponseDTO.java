package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DocumentoCobroResponseDTO {
    private Long id;
    private String tipo;
    private String estado;
    private BigDecimal subtotal;
    private BigDecimal total;
    private BigDecimal monto;
    private List<Long> detalleIds;
}
