package com.rutadelsabor.core.models.entities;

import com.rutadelsabor.core.models.enums.EstadoCaja;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_caja")
@Getter
@Setter
public class SesionCaja extends BaseSedeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false)
    private Usuario cajero;

    @Column(name = "fecha_apertura")
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_inicial", nullable = false)
    private BigDecimal montoInicial;

    @Column(name = "monto_final_calculado")
    private BigDecimal montoFinalCalculado = BigDecimal.ZERO;

    @Column(name = "monto_final_declarado")
    private BigDecimal montoFinalDeclarado;

    @Column(name = "diferencia", insertable = false, updatable = false)
    private BigDecimal diferencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCaja estado;
}