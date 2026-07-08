package com.rutadelsabor.core.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PedidoRequestDTO {
    
    private String tipoConsumo;
    private String mesa;
    private String notasGenerales;
    
    // FASE 6: Sede opcional para que el ADMIN_EMPRESA pueda elegir en qué local crea el pedido.
    // Para roles operativos (Mozo, Cajero), este campo se ignora.
    private Long sedeId; 

    private List<PedidoItemDTO> items;

    @Data
    public static class PedidoItemDTO {
        private Long productoId;
        private Integer cantidad;
        private String notasPreparacion;
    }
}