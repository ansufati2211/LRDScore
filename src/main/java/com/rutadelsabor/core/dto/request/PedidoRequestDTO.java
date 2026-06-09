package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PedidoRequestDTO {
    private String mesa;
    private String tipoConsumo;
    private BigDecimal total;
    private List<PedidoItemDTO> items;

    @Getter
    @Setter
    public static class PedidoItemDTO {
        private Long productoId;
        private Integer cantidad;
        private BigDecimal subtotal;
        // NUEVO: Captura las notas para la cocina (Resuelve Hallazgo KDS)
        private String notasPreparacion; 
    }
}