package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class MargenVentasDTO {

    private BigDecimal ingresosTotales;
    private BigDecimal costoVentas;
    private BigDecimal utilidadBruta;
    private BigDecimal margenBrutoPct;
    private BigDecimal costoMerma;
    private List<MargenProductoDTO> desglosePorProducto;
    private List<MargenCategoriaDTO> desglosePorCategoria;

    @Getter
    @Setter
    public static class MargenProductoDTO {
        private Long productoId;
        private String producto;
        private BigDecimal ingresos;
        private BigDecimal costoVentas;
        private BigDecimal utilidadBruta;
        private BigDecimal margenPct;
        private Boolean esEstimado;
    }

    @Getter
    @Setter
    public static class MargenCategoriaDTO {
        private Long categoriaId;
        private String categoria;
        private BigDecimal ingresos;
        private BigDecimal costoVentas;
        private BigDecimal utilidadBruta;
        private BigDecimal margenPct;
    }
}
