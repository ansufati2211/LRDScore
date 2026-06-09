package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    // Método para la llamada nativa del SP (Ya lo tenías)
    @SuppressWarnings("squid:S107")
    @org.springframework.data.jpa.repository.query.Procedure(procedureName = "sp_registrar_venta_y_descontar_stock")
    void procesarPagoYDescontarStock(
        @org.springframework.data.repository.query.Param("p_pedido_id") Long pedidoId,
        @org.springframework.data.repository.query.Param("p_cajero_id") Long cajeroId,
        @org.springframework.data.repository.query.Param("p_sesion_caja_id") Long sesionCajaId,
        @org.springframework.data.repository.query.Param("p_metodo_pago") String metodoPago,
        @org.springframework.data.repository.query.Param("p_monto") java.math.BigDecimal monto,
        @org.springframework.data.repository.query.Param("p_numero_yape") String numeroYape,
        @org.springframework.data.repository.query.Param("p_ultimos_digitos") String ultimosDigitos,
        @org.springframework.data.repository.query.Param("p_titular") String titular
    );

    // NUEVO: Para que el frontend pueda pintar las mesas ocupadas
    List<Pedido> findByEstadoActualInOrderByCreatedAtDesc(List<EstadoPedido> estados);
}