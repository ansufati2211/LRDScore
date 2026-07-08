package com.rutadelsabor.core.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "kardex_movimientos")
@Getter
@Setter
public class KardexMovimiento extends BaseSedeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private String tipoMovimiento;

    @Column(name = "cantidad", nullable = false)
    private BigDecimal cantidad;

    @Column(name = "stock_anterior", nullable = false)
    private BigDecimal stockAnterior;

    @Column(name = "stock_posterior", nullable = false)
    private BigDecimal stockPosterior;

    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;
}