package com.rutadelsabor.core.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PedidoRequestDTO {
    private String mesa;
    private String tipoConsumo;
    private String notasGenerales;
    private List<PedidoItemDTO> items;

    @Getter
    @Setter
    public static class PedidoItemDTO {
        private Long productoId;
        private Integer cantidad;
        private String notasPreparacion; 
    }
}