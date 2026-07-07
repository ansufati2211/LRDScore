package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.RecetaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecetaDetalleRepository extends JpaRepository<RecetaDetalle, Long> {
    List<RecetaDetalle> findByProductoId(Long productoId);
}