package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.RecetaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecetaDetalleRepository extends JpaRepository<RecetaDetalle, Long> {
    List<RecetaDetalle> findByProductoId(Long productoId);
}