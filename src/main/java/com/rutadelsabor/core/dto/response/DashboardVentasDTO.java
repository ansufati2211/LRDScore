package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DashboardVentasDTO {
    private BigDecimal ingresosTotalesMensuales;
    private Integer pedidosTotalesMensuales;
    private List<DetalleDiaDTO> detalleDiario;

    @Getter
    @Setter
    public static class DetalleDiaDTO {
        private String fecha;
        private BigDecimal ingresos;
        private Integer pedidos;
    }
}