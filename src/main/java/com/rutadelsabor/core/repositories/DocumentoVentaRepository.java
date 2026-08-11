package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.DocumentoVenta;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface DocumentoVentaRepository extends JpaRepository<DocumentoVenta, Long> {

    List<DocumentoVenta> findByPedidoId(Long pedidoId);
    List<DocumentoVenta> findByDocumentoCobroId(Long documentoCobroId);
}
