package com.rutadelsabor.core.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "insumo_sede")
@IdClass(InsumoSedeId.class)
public class InsumoSede {

    private static final Clock UTC_CLOCK = Clock.systemUTC();

    @Id
    @Column(name = "insumo_id")
    private Long insumoId;

    @Id
    @Column(name = "sede_id")
    private Long sedeId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "stock_actual", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", precision = 12, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "stock_reservado", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockReservado = BigDecimal.ZERO;

    @Column(name = "costo_unitario", precision = 10, scale = 2)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relaciones de solo lectura para evitar cascadas accidentales
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", insertable = false, updatable = false)
    private Insumo insumo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", insertable = false, updatable = false)
    private Sede sede;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(UTC_CLOCK);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(UTC_CLOCK);
    }
}