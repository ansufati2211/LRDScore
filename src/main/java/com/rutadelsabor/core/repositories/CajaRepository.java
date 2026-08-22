package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.enums.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<SesionCaja, Long> {
    
    Optional<SesionCaja> findBySedeIdAndCajeroIdAndEstado(Long sedeId, Long cajeroId, EstadoCaja estado);
    
    List<SesionCaja> findBySedeIdAndCajeroIdOrderByFechaAperturaDesc(Long sedeId, Long cajeroId);
    
    Optional<SesionCaja> findByCajeroIdAndEstado(Long cajeroId, EstadoCaja estado);
    
    List<SesionCaja> findByCajeroIdOrderByFechaAperturaDesc(Long cajeroId);

    @Query("SELECT s FROM SesionCaja s JOIN FETCH s.cajero c WHERE s.sedeId = :sedeId AND s.fechaApertura >= :inicio AND s.fechaApertura <= :fin ORDER BY s.fechaApertura DESC")
    List<SesionCaja> findBySedeIdAndFechaAperturaBetween(@Param("sedeId") Long sedeId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("SELECT s FROM SesionCaja s JOIN FETCH s.cajero c WHERE s.empresaId = :empresaId AND s.fechaApertura >= :inicio AND s.fechaApertura <= :fin ORDER BY s.fechaApertura DESC")
    List<SesionCaja> findByEmpresaIdAndFechaAperturaBetween(@Param("empresaId") Long empresaId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    List<SesionCaja> findByEstado(EstadoCaja estado);
}