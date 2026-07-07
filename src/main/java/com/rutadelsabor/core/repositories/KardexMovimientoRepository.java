package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.KardexMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KardexMovimientoRepository extends JpaRepository<KardexMovimiento, Long> {
    List<KardexMovimiento> findByInsumoIdOrderByCreatedAtDesc(Long insumoId);

    List<KardexMovimiento> findByPedidoIdAndTipoMovimiento(Long pedidoId, String tipoMovimiento);
}