package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PedidoActivoResponseDTO {
    private Long id;
    private String mozo;
    private String tipoConsumo;
    private String mesa;
    private String estadoActual;
    private BigDecimal descuento;
    private BigDecimal total;
    private LocalDateTime fechaCreacion;
    private List<DetallePlanoDTO> items;

    @Getter
    @Setter
    public static class DetallePlanoDTO {
        private Long productoId;
        private String nombreProducto;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
        private String notasPreparacion;
    }
}