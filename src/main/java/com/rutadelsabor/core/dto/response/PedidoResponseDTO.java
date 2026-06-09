package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PedidoResponseDTO {
    private Long id;
    private String tipoConsumo;
    private String identificadorMesaReferencia;
    private String estadoActual;
    private BigDecimal total;
    private Date createdAt;
    private List<DetalleDTO> detalles;

    @Getter
    @Setter
    public static class DetalleDTO {
        private Long productoId;
        private String nombreProducto; // Agregamos el nombre para que el Frontend lo pinte directo
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }
}