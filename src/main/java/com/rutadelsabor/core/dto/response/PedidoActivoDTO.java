// src/main/java/com/rutadelsabor/core/dto/response/PedidoActivoDTO.java
package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PedidoActivoDTO {

    private Long id;
    private Integer numeroOrden;
    private String tipoConsumo;
    private String identificadorMesaReferencia;
    private String estadoActual;
    private BigDecimal subtotal;
    private BigDecimal total;
    private String notasGenerales;
    private LocalDateTime createdAt;
    private String nombreMozo;
    private List<DetalleActivoDTO> detalles;

    @Getter
    @Setter
    public static class DetalleActivoDTO {
        private Long id;
        private Long productoId;
        private String nombreProducto;
        private BigDecimal precioUnitario;
        private Integer cantidad;
        private BigDecimal subtotal;
        private String notasPreparacion;
    }
}