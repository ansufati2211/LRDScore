package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.VwKdsCocina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VwKdsCocinaRepository extends JpaRepository<VwKdsCocina, Long> {
    
    // Busca los pedidos que estén en ciertos estados y los ordena por el más antiguo primero
    List<VwKdsCocina> findByEstadoPedidoInOrderByFechaPedidoAsc(List<String> estados);
}