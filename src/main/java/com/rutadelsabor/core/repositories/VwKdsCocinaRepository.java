package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VwKdsCocinaRepository extends JpaRepository<VwKdsCocina, Long> {

    // FIFO: los pedidos más antiguos aparecen primero en cocina
    List<VwKdsCocina> findByEstadoPedidoInOrderByHoraIngresoAsc(List<String> estados);
}