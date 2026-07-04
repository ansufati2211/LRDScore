package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.DocumentoCobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoCobroRepository extends JpaRepository<DocumentoCobro, Long> {
    List<DocumentoCobro> findByPedidoId(Long pedidoId);
}
