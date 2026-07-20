package com.rutadelsabor.core.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "documentos_cobro")
public class DocumentoCobro extends BaseSedeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado", nullable = false, length = 10)
    private String estado = "PENDIENTE";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "documentos_cobro_detalle",
        joinColumns = @JoinColumn(name = "documento_cobro_id"),
        inverseJoinColumns = @JoinColumn(name = "pedido_detalle_id")
    )
    private List<PedidoDetalle> detalles = new ArrayList<>();
}