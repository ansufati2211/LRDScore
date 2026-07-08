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

    @Query("SELECT isede FROM InsumoSede isede JOIN FETCH isede.insumo i WHERE isede.sedeId = :sedeId AND isede.stockActual <= isede.stockMinimo AND i.estadoRegistro = true ORDER BY i.nombre")
    List<InsumoSede> findInsumosConStockBajoPorSede(@Param("sedeId") Long sedeId);

    // FASE 4: Método global sin filtro de sede para ADMIN_EMPRESA
    @Query("SELECT isede FROM InsumoSede isede JOIN FETCH isede.insumo i WHERE isede.stockActual <= isede.stockMinimo AND i.estadoRegistro = true ORDER BY i.nombre")
    List<InsumoSede> findInsumosConStockBajo();
}