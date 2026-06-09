package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {
    // Query optimizada indexada por base de datos
    List<Receta> findByProductoId(Long productoId);
}