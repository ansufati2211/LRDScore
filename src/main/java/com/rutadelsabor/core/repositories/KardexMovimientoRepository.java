package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.KardexMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KardexMovimientoRepository extends JpaRepository<KardexMovimiento, Long> {
    List<KardexMovimiento> findByInsumoIdOrderByCreatedAtDesc(Long insumoId);

    List<KardexMovimiento> findByPedidoIdAndTipoMovimiento(Long pedidoId, String tipoMovimiento);
}