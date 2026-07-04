package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DocumentoCobroRequestDTO {
    private String tipo;          // "ITEMS" | "MONTO"
    private List<Long> detalleIds; // requerido para tipo ITEMS
    private BigDecimal monto;      // requerido para tipo MONTO
}
