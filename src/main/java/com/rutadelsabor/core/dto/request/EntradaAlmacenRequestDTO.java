package com.rutadelsabor.core.dto.request;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class EntradaAlmacenRequestDTO {
    private Long insumoId;
    private BigDecimal cantidad;
    private BigDecimal costoUnitario;
    private String observacion;
    private Long sedeId;
}