package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.InsumoSede;
import com.rutadelsabor.core.models.entities.InsumoSedeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsumoSedeRepository extends JpaRepository<InsumoSede, InsumoSedeId> {
    
    Optional<InsumoSede> findBySedeIdAndInsumoId(Long sedeId, Long insumoId);
    
    List<InsumoSede> findBySedeId(Long sedeId);

    // FIX: El método que antes estaba en InsumoRepository ahora vive aquí y cruza con la sede
    @Query("SELECT is FROM InsumoSede is JOIN FETCH is.insumo i WHERE is.sedeId = :sedeId AND is.stockActual <= is.stockMinimo AND i.estadoRegistro = true ORDER BY i.nombre")
    List<InsumoSede> findInsumosConStockBajoPorSede(@Param("sedeId") Long sedeId);
}