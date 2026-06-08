package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class SesionCajaRequestDTO {
    private Long cajeroId;
    private BigDecimal montoInicial;
    private BigDecimal montoFinalDeclarado; // Este solo se usará al cerrar la caja
}