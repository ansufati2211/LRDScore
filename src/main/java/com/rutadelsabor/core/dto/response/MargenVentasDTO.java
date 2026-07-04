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
    // E5-2: costo de ítems cancelados que ya habían consumido inventario
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
        // R5-4: true cuando el costo proviene de costo_referencial (producto sin receta)
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
