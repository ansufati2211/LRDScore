package com.rutadelsabor.core.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vw_dashboard_ventas")
@Getter
@Setter
@Immutable
public class VwDashboardVentas {

    // La vista genera este ID con ROW_NUMBER() OVER(ORDER BY fecha, empresa_id).
    // Garantiza unicidad multi-tenant: dos empresas el mismo día tienen IDs distintos.
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "total_ingresos")
    private BigDecimal totalIngresos;

    @Column(name = "cantidad_pedidos")
    private Long cantidadPedidos;

    @Column(name = "empresa_id")
    private Long empresaId;
}