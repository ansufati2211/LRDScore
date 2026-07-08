package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.KardexMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KardexMovimientoRepository extends JpaRepository<KardexMovimiento, Long> {
    
    // Método original (se mantiene para la reserva)
    List<KardexMovimiento> findByPedidoIdAndTipoMovimiento(Long pedidoId, String tipoMovimiento);

    // NUEVO MÉTODO: El que está pidiendo InventarioServiceImpl
    List<KardexMovimiento> findBySedeIdAndInsumoIdOrderByCreatedAtDesc(Long sedeId, Long insumoId);
}