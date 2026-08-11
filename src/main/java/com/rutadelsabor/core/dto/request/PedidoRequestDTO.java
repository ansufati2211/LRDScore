package com.rutadelsabor.core.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PedidoRequestDTO {
    
    private String tipoConsumo;
    private String mesa;
    private String notasGenerales;
    private Long sedeId; 

    private List<PedidoItemDTO> items;

    @Data
    public static class PedidoItemDTO {
        private Long productoId;
        private Integer cantidad;
        private String notasPreparacion;
    }
}