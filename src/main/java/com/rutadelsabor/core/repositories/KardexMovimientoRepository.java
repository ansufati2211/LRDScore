package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.KardexMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KardexMovimientoRepository extends JpaRepository<KardexMovimiento, Long> {

    List<KardexMovimiento> findByPedidoIdAndTipoMovimiento(Long pedidoId, String tipoMovimiento);

    // Búsqueda para un local específico
    List<KardexMovimiento> findBySedeIdAndInsumoIdOrderByCreatedAtDesc(Long sedeId, Long insumoId);

    // FASE 4: Búsqueda global para ADMIN_EMPRESA
    List<KardexMovimiento> findByInsumoIdOrderByCreatedAtDesc(Long insumoId);
}