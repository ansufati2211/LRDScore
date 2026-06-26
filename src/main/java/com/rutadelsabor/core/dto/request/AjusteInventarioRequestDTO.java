package com.rutadelsabor.core.dto.request;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class AjusteInventarioRequestDTO {
    private Long insumoId;
    private BigDecimal cantidad;
    private Boolean esPositivo; // true = sobra stock, false = falta stock
    private String motivo;
}