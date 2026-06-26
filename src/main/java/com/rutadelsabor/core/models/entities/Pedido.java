package com.rutadelsabor.core.models.entities;

import com.rutadelsabor.core.models.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
public class Pedido extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mozo_id", nullable = false)
    private Usuario mozo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id")
    private SesionCaja sesionCaja;

    @Column(name = "tipo_consumo", nullable = false, length = 50)
    private String tipoConsumo;

    @Column(name = "identificador_mesa_referencia", length = 50)
    private String identificadorMesaReferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 50)
    private EstadoPedido estadoActual;

    @Column(name = "numero_orden")
    private Integer numeroOrden;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "descuento")
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "total", nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "notas_generales", columnDefinition = "TEXT")
    private String notasGenerales;

    @Column(name = "estado_registro")
    private Boolean estadoRegistro = true;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoDetalle> detalles = new ArrayList<>();
}