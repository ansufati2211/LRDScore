package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Los SP se mantienen iguales porque la lógica de Sede ya se inyectó en el SQL de PostgreSQL
    @Procedure(procedureName = "sp_iniciar_preparacion")
    void iniciarPreparacionYDescontarStock(
        @Param("p_pedido_id") Long pedidoId,
        @Param("p_usuario_id") Long usuarioId
    );

    @SuppressWarnings("squid:S107")
    @Procedure(procedureName = "sp_registrar_pago")
    void registrarPago(
        @Param("p_pedido_id") Long pedidoId,
        @Param("p_sesion_caja_id") Long sesionCajaId,
        @Param("p_metodo_pago") String metodoPago,
        @Param("p_monto") BigDecimal monto,
        @Param("p_numero_yape") String numeroYape,
        @Param("p_ultimos_digitos") String ultimosDigitos,
        @Param("p_titular") String titular
    );

    // FIX: Obligamos a Spring Data a buscar también por SedeId
    List<Pedido> findBySedeIdAndEstadoActualInOrderByCreatedAtDesc(Long sedeId, List<EstadoPedido> estados);

    List<Pedido> findBySedeIdAndEstadoActualInAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long sedeId, List<EstadoPedido> estados, LocalDateTime inicio, LocalDateTime fin);

 // Método global para el Scheduler (busca en todas las sedes)
    List<Pedido> findByEstadoActualAndCreatedAtBefore(EstadoPedido estado, java.time.LocalDateTime fecha);           
}