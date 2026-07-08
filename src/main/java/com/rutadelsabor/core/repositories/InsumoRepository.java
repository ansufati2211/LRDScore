package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;

// Ya no hay queries de stock aquí, porque el stock ahora le pertenece a la Sede (InsumoSede)
public interface InsumoRepository extends JpaRepository<Insumo, Long> {
}