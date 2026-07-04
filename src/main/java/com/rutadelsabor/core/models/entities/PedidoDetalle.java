package com.rutadelsabor.core.models.entities;

import com.rutadelsabor.core.models.enums.EstadoItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "pedidos_detalle")
public class PedidoDetalle extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "notas_preparacion", columnDefinition = "TEXT")
    private String notasPreparacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_item", nullable = false, length = 20)
    private EstadoItem estadoItem = EstadoItem.PENDIENTE;

    @Column(name = "numero_comanda", nullable = false)
    private Integer numeroComanda = 1;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    // R5-1: snapshot del costo promedio ponderado al consumir (EN_PREPARACION). No se recalcula.
    @Column(name = "costo_unitario_consumido", precision = 12, scale = 4)
    private BigDecimal costoUnitarioConsumido;
}
