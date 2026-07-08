package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.enums.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<SesionCaja, Long> {
    
    // FIX: Filtramos siempre por SedeId
    Optional<SesionCaja> findBySedeIdAndCajeroIdAndEstado(Long sedeId, Long cajeroId, EstadoCaja estado);
    
    List<SesionCaja> findBySedeIdAndCajeroIdOrderByFechaAperturaDesc(Long sedeId, Long cajeroId);
    
// FASE 4: Métodos globales (Sin sede_id) para ROLE_ADMIN_EMPRESA
    Optional<SesionCaja> findByCajeroIdAndEstado(Long cajeroId, EstadoCaja estado);
    
    List<SesionCaja> findByCajeroIdOrderByFechaAperturaDesc(Long cajeroId);}