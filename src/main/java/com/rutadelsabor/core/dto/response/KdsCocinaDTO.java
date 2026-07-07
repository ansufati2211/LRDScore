package com.rutadelsabor.core.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class KdsCocinaDTO {
    private Long detalleId;
    private Long pedidoId;
    private String mesa;
    private String producto;
    private Integer cantidad;
    private String estadoPedido;
    private String notasPreparacion;
    private LocalDateTime fechaPedido;
    
    // Estos campos adicionales pueden ser útiles más adelante cuando conectes el frontend
    private String tiempoEsperaTranscurrido; 
}