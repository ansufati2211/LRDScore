package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Insumo;
import com.rutadelsabor.core.models.entities.InsumoSede;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    @Query("SELECT is FROM InsumoSede is JOIN FETCH is.insumo i WHERE is.stockActual <= is.stockMinimo AND i.estadoRegistro = true ORDER BY i.nombre")
    List<InsumoSede> findInsumosConStockBajo();
}