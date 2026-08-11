package com.rutadelsabor.core.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class KdsCocinaDTO {
    private Long pedidoId;
    private Integer numeroOrden; 
    private String tipoConsumo;  
    private String mesa;
    private String estadoPedido;
    private String notasGenerales; 
    private OffsetDateTime horaIngreso; 
    private Double minutosTranscurridos; 
    private List<KdsItemDTO> items;

    @Data
public static class KdsItemDTO {
        private Long detalleId;
        private Long productoId; 
        private String producto;
        private Integer cantidad;
        private String notasPreparacion;
        private Integer tiempoPreparacionMinutos;
        private String estadoItem;
        private Integer numeroComanda;
        private String categoriaNombre; 
    }
}