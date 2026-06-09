package com.rutadelsabor.core.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vw_dashboard_ventas")
@Getter
public class VwDashboardVentas {

    @Id
    @Column(name = "fecha")
    private LocalDate fecha; // En vistas, usamos una columna única como ID virtual

    @Column(name = "total_ingresos")
    private BigDecimal totalIngresos;

    @Column(name = "cantidad_pedidos")
    private Integer cantidadPedidos;

    @Column(name = "empresa_id")
    private Long empresaId;
}