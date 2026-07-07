package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    @Query("SELECT i FROM Insumo i WHERE i.stockActual <= i.stockMinimo AND i.estadoRegistro = true ORDER BY i.nombre")
    List<Insumo> findInsumosConStockBajo();
}