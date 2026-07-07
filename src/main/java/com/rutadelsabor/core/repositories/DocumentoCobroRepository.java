package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.DocumentoCobro;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface DocumentoCobroRepository extends JpaRepository<DocumentoCobro, Long> {
    List<DocumentoCobro> findByPedidoId(Long pedidoId);
}
