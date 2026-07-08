package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VwKdsCocinaRepository extends JpaRepository<VwKdsCocina, Long> {

    // FIX: Cada cocina física debe ver solo sus propios pedidos
    List<VwKdsCocina> findBySedeIdAndEstadoPedidoInOrderByHoraIngresoAsc(Long sedeId, List<String> estados);
}