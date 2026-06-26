package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Pedido;
import com.rutadelsabor.core.models.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    // NUEVO SP: Descuenta stock y registra en Kardex atómicamente al enviar a cocina
    @Procedure(procedureName = "sp_iniciar_preparacion")
    void iniciarPreparacionYDescontarStock(
        @Param("p_pedido_id") Long pedidoId,
        @Param("p_usuario_id") Long usuarioId
    );

    // NUEVO SP: Solo registra el pago (Soporta pagos múltiples/mixtos desde el servicio)
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

    // Para que el frontend pueda pintar las mesas ocupadas
    List<Pedido> findByEstadoActualInOrderByCreatedAtDesc(List<EstadoPedido> estados);
}