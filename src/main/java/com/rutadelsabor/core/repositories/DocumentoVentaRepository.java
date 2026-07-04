package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.DocumentoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoVentaRepository extends JpaRepository<DocumentoVenta, Long> {

    // @TenantId en BaseTenantEntity filtra automáticamente por empresa_id
    List<DocumentoVenta> findByPedidoId(Long pedidoId);
    List<DocumentoVenta> findByDocumentoCobroId(Long documentoCobroId);
}
