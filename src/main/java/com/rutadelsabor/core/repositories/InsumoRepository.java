package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.InsumoSede;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Ya no hay queries de stock aquí, porque el stock ahora le pertenece a la Sede (InsumoSede)
public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    // FASE 4: Método global consolidado de insumos con stock bajo para toda la franquicia
    @Query("SELECT is FROM InsumoSede is JOIN FETCH is.insumo i WHERE is.stockActual <= is.stockMinimo AND i.estadoRegistro = true ORDER BY i.nombre")
    List<InsumoSede> findInsumosConStockBajo();
}