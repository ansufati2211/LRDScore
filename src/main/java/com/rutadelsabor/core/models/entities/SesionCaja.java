package com.rutadelsabor.core.models.entities;

import com.rutadelsabor.core.models.enums.EstadoCaja;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "sesiones_caja")
public class SesionCaja extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Usuario cajero;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_apertura")
    private Date fechaApertura;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_cierre")
    private Date fechaCierre;

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "monto_final_calculado", precision = 10, scale = 2)
    private BigDecimal montoFinalCalculado;

    @Column(name = "monto_final_declarado", precision = 10, scale = 2)
    private BigDecimal montoFinalDeclarado;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoCaja estado = EstadoCaja.ABIERTA;
}