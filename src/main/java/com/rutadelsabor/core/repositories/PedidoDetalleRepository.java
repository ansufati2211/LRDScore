package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.PedidoDetalle;
import com.rutadelsabor.core.models.enums.EstadoItem;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {

    // Módulo 5 — R5-2/R5-3: ítems vendidos con costo snapshot en pedidos PAGADO del período
    @Query("""
        SELECT pd FROM PedidoDetalle pd
        JOIN FETCH pd.pedido p
        JOIN FETCH pd.producto pr
        JOIN FETCH pr.categoria
        WHERE p.estadoActual = :estado
          AND p.createdAt >= :inicio
          AND p.createdAt <= :fin
          AND pd.estadoItem <> :cancelado
          AND pd.costoUnitarioConsumido IS NOT NULL
        """)
    List<PedidoDetalle> findDetallesConCostoPorPeriodo(
            @Param("estado") EstadoPedido estado,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("cancelado") EstadoItem cancelado);

    // Módulo 5 — E5-2: ítems cancelados con consumo previo (merma) en el período
    @Query("""
        SELECT pd FROM PedidoDetalle pd
        JOIN FETCH pd.pedido p
        WHERE p.createdAt >= :inicio
          AND p.createdAt <= :fin
          AND pd.estadoItem = :cancelado
          AND pd.costoUnitarioConsumido IS NOT NULL
        """)
    List<PedidoDetalle> findDetallesMermaConCostoPorPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("cancelado") EstadoItem cancelado);
}
