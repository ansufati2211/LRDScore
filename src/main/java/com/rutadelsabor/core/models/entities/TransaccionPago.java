package com.rutadelsabor.core.models.entities;

import com.rutadelsabor.core.models.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "transacciones_pago")
public class TransaccionPago extends BaseTenantEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id")
    private SesionCaja sesionCaja;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 50)
    private MetodoPago metodoPago;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "estado_pago", length = 50)
    private String estadoPago = "COMPLETADO";

    @Column(name = "titular_tarjeta", length = 100)
    private String titularTarjeta;

    @Column(name = "ultimos_digitos_tarjeta", length = 4)
    private String ultimosDigitosTarjeta;

    @Column(name = "numero_yape", length = 20)
    private String numeroYape;

    @Column(name = "referencia_pasarela", length = 255)
    private String referenciaPasarela;
}