package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.enums.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<SesionCaja, Long> {
    Optional<SesionCaja> findByCajeroIdAndEstado(Long cajeroId, EstadoCaja estado);
    List<SesionCaja> findByCajeroIdOrderByFechaAperturaDesc(Long cajeroId);
}
