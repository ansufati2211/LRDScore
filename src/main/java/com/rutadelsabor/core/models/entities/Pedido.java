package com.rutadelsabor.core.models.entities;

import com.rutadelsabor.core.models.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pedidos")
public class Pedido extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mozo_id", nullable = false)
    private Usuario mozo;

    // Se autogenera en PostgreSQL (SERIAL)
    @Column(name = "numero_orden", insertable = false, updatable = false)
    private Integer numeroOrden;

    @Column(name = "tipo_consumo", nullable = false, length = 20)
    private String tipoConsumo; // MESA, DELIVERY, PARA_LLEVAR

    @Column(name = "identificador_mesa_referencia", length = 100)
    private String identificadorMesaReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 30)
    private EstadoPedido estadoActual = EstadoPedido.BORRADOR;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "costo_delivery", precision = 10, scale = 2)
    private BigDecimal costoDelivery = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "notas_generales", columnDefinition = "TEXT")
    private String notasGenerales;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;

    // Relación para traer los platos de la orden
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PedidoDetalle> detalles = new ArrayList<>();
}