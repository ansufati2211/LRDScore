package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DocumentoCobroRequestDTO {
    private String tipo;  
    private List<Long> detalleIds;
    private BigDecimal monto;     
}
