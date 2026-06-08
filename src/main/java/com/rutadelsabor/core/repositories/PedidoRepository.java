package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Procedure(procedureName = "sp_registrar_venta_y_descontar_stock")
    void procesarPagoYDescontarStock(
        @Param("p_pedido_id") Long pedidoId,
        @Param("p_cajero_id") Long cajeroId,
        @Param("p_sesion_caja_id") Long sesionCajaId,
        @Param("p_metodo_pago") String metodoPago,
        @Param("p_monto") BigDecimal monto,
        @Param("p_numero_yape") String numeroYape,
        @Param("p_ultimos_digitos") String ultimosDigitos,
        @Param("p_titular") String titular
    );
}