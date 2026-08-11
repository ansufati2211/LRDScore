package com.rutadelsabor.core.dto.request;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class AjusteInventarioRequestDTO {
    private Long insumoId;
    private BigDecimal cantidad;
    private Boolean esPositivo; 
    private String motivo;
    private Long sedeId;
}