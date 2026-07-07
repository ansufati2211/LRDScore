package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.DocumentoVenta;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface DocumentoVentaRepository extends JpaRepository<DocumentoVenta, Long> {

    // @TenantId en BaseTenantEntity filtra automáticamente por empresa_id
    List<DocumentoVenta> findByPedidoId(Long pedidoId);
    List<DocumentoVenta> findByDocumentoCobroId(Long documentoCobroId);
}
