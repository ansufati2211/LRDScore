package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DashboardVentasDTO {
    private BigDecimal ingresosTotalesMensuales;
    private Long pedidosTotalesMensuales;
    private List<DetalleDiaDTO> detalleDiario;
    
    // Listas inyectadas para los nuevos gráficos
    private List<ProductoTopDTO> productosMasVendidos;
    private List<CategoriaTopDTO> ventasPorCategoria;

    @Getter
    @Setter
    public static class DetalleDiaDTO {
        private String fecha;
        private BigDecimal ingresos;
        private Long pedidos;
    }

    @Getter
    @Setter
    public static class ProductoTopDTO {
        private String producto;
        private Long cantidadVendida;
    }

    @Getter
    @Setter
    public static class CategoriaTopDTO {
        private String categoria;
        private BigDecimal ingresosTotales;
    }
}