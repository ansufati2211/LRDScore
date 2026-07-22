package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.PedidoDetalle;
import com.rutadelsabor.core.models.enums.EstadoItem;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {

    @Query("SELECT d FROM PedidoDetalle d JOIN FETCH d.producto p JOIN FETCH p.categoria c JOIN FETCH d.pedido ped " +
           "WHERE ped.estadoActual = :estadoPedido AND ped.createdAt >= :inicio AND ped.createdAt <= :fin " +
           "AND d.estadoItem != :estadoExcluido")
    List<PedidoDetalle> findDetallesConCostoPorPeriodo(
            @Param("estadoPedido") EstadoPedido estadoPedido,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadoExcluido") EstadoItem estadoExcluido);

    @Query("SELECT d FROM PedidoDetalle d JOIN FETCH d.producto p JOIN FETCH p.categoria c JOIN FETCH d.pedido ped " +
           "WHERE ped.createdAt >= :inicio AND ped.createdAt <= :fin AND d.estadoItem = :estadoCancelado")
    List<PedidoDetalle> findDetallesMermaConCostoPorPeriodo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadoCancelado") EstadoItem estadoCancelado);

    @Query("SELECT d FROM PedidoDetalle d JOIN FETCH d.producto p JOIN FETCH p.categoria c JOIN FETCH d.pedido ped " +
           "WHERE ped.sedeId = :sedeId AND ped.estadoActual = :estadoPedido AND ped.createdAt >= :inicio AND ped.createdAt <= :fin " +
           "AND d.estadoItem != :estadoExcluido")
    List<PedidoDetalle> findDetallesConCostoPorPeriodoYSede(
            @Param("sedeId") Long sedeId,
            @Param("estadoPedido") EstadoPedido estadoPedido,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadoExcluido") EstadoItem estadoExcluido);

    @Query("SELECT d FROM PedidoDetalle d JOIN FETCH d.producto p JOIN FETCH p.categoria c JOIN FETCH d.pedido ped " +
           "WHERE ped.sedeId = :sedeId AND ped.createdAt >= :inicio AND ped.createdAt <= :fin AND d.estadoItem = :estadoCancelado")
    List<PedidoDetalle> findDetallesMermaConCostoPorPeriodoYSede(
            @Param("sedeId") Long sedeId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estadoCancelado") EstadoItem estadoCancelado);

    // ==========================================
    // TOP PRODUCTOS MÁS VENDIDOS
    // ==========================================
    @Query("SELECT d.producto.nombre, SUM(d.cantidad) FROM PedidoDetalle d JOIN d.pedido ped " +
           "WHERE ped.sedeId = :sedeId AND ped.estadoActual IN ('PAGADO', 'ENTREGADO') " +
           "AND ped.createdAt >= :inicio AND ped.createdAt <= :fin " +
           "AND d.estadoItem != 'CANCELADO' " +
           "GROUP BY d.producto.nombre ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> findTopProductosVendidosPorSede(
            @Param("sedeId") Long sedeId, @Param("inicio") LocalDateTime inicio, 
            @Param("fin") LocalDateTime fin, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT d.producto.nombre, SUM(d.cantidad) FROM PedidoDetalle d JOIN d.pedido ped " +
           "WHERE ped.empresaId = :empresaId AND ped.estadoActual IN ('PAGADO', 'ENTREGADO') " +
           "AND ped.createdAt >= :inicio AND ped.createdAt <= :fin " +
           "AND d.estadoItem != 'CANCELADO' " +
           "GROUP BY d.producto.nombre ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> findTopProductosVendidosGlobal(
            @Param("empresaId") Long empresaId, @Param("inicio") LocalDateTime inicio, 
            @Param("fin") LocalDateTime fin, org.springframework.data.domain.Pageable pageable);

    // ==========================================
    // VENTAS POR CATEGORÍA
    // ==========================================
    @Query("SELECT c.nombre, SUM(d.subtotal) FROM PedidoDetalle d JOIN d.pedido ped JOIN d.producto p JOIN p.categoria c " +
           "WHERE ped.sedeId = :sedeId AND ped.estadoActual IN ('PAGADO', 'ENTREGADO') " +
           "AND ped.createdAt >= :inicio AND ped.createdAt <= :fin " +
           "AND d.estadoItem != :estadoExcluido " +
           "GROUP BY c.nombre ORDER BY SUM(d.subtotal) DESC")
    List<Object[]> findVentasPorCategoriaSede(
            @Param("sedeId") Long sedeId, @Param("inicio") LocalDateTime inicio, 
            @Param("fin") LocalDateTime fin, @Param("estadoExcluido") EstadoItem estadoExcluido);

    @Query("SELECT c.nombre, SUM(d.subtotal) FROM PedidoDetalle d JOIN d.pedido ped JOIN d.producto p JOIN p.categoria c " +
           "WHERE ped.empresaId = :empresaId AND ped.estadoActual IN ('PAGADO', 'ENTREGADO') " +
           "AND ped.createdAt >= :inicio AND ped.createdAt <= :fin " +
           "AND d.estadoItem != :estadoExcluido " +
           "GROUP BY c.nombre ORDER BY SUM(d.subtotal) DESC")
    List<Object[]> findVentasPorCategoriaGlobal(
            @Param("empresaId") Long empresaId, @Param("inicio") LocalDateTime inicio, 
            @Param("fin") LocalDateTime fin, @Param("estadoExcluido") EstadoItem estadoExcluido);
}