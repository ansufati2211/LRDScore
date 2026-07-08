package com.rutadelsabor.core.dto.request;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class MermaRequestDTO {
    private Long insumoId;
    private BigDecimal cantidad;
    private String motivo; // Obligatorio
    private Long sedeId;
}