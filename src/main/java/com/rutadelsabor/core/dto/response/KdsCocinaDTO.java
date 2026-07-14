package com.rutadelsabor.core.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class KdsCocinaDTO {
    private Long pedidoId;
    private Integer numeroOrden; // FIX: añadido
    private String tipoConsumo;  // FIX: añadido
    private String mesa;
    private String estadoPedido;
    private String notasGenerales; // FIX: añadido
    private OffsetDateTime horaIngreso; // FIX: añadido
    private Double minutosTranscurridos; // FIX: añadido
    private List<KdsItemDTO> items;

    // FIX: Clase anidada interna que el servicio necesita para procesar los detalles
    @Data
public static class KdsItemDTO {
        private Long detalleId;
        private Long productoId; // NUEVO: Para buscar la receta
        private String producto;
        private Integer cantidad;
        private String notasPreparacion;
        private Integer tiempoPreparacionMinutos;
        private String estadoItem;
        private Integer numeroComanda;
        private String categoriaNombre; // NUEVO: Para filtrar por Estaciones (ej. Bebidas, Parrilla)
    }
}