package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VwKdsCocinaRepository extends JpaRepository<VwKdsCocina, Long> {

    // FIFO: los pedidos más antiguos aparecen primero en cocina
    List<VwKdsCocina> findByEstadoPedidoInOrderByHoraIngresoAsc(List<String> estados);
}